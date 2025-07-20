package dev.jpleatherland.weighttracker.viewmodel

import android.util.Log
import androidx.health.connect.client.*
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jpleatherland.weighttracker.BuildConfig
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalProgress
import dev.jpleatherland.weighttracker.data.GoalRepository
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.data.WeightRepository
import dev.jpleatherland.weighttracker.util.GoalCalculations
import dev.jpleatherland.weighttracker.util.GoalCalculations.estimateCalories
import dev.jpleatherland.weighttracker.util.GoalCalculations.reconstructRateKgPerWeek
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit

class WeightViewModel(
    private val repository: WeightRepository,
    private val goalRepository: GoalRepository,
    val healthConnectClient: HealthConnectClient,
) : ViewModel() {
    val entries: StateFlow<List<WeightEntry>> =
        repository
            .getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(
        weight: Double?,
        calories: Int?,
    ) {
        viewModelScope.launch {
            val entry = WeightEntry(weight = weight, calories = calories)
            repository.insert(entry)
        }
    }

    fun updateEntry(entry: WeightEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }

    val goal: StateFlow<Goal?> =
        goalRepository
            .getLatestGoal()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.insert(goal)
        }
    }

    fun clearGoal() {
        viewModelScope.launch {
            goalRepository.clearGoal()
        }
    }

    private val sevenDaysAgo: Long
        get() = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    val sevenDayAvgWeight: StateFlow<Double?> =
        entries
            .map { list ->
                list
                    .filter { it.weight != null && it.date >= sevenDaysAgo }
                    .mapNotNull { it.weight }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null,
            )

    val estimatedMaintenanceCalories: StateFlow<Int?> =
        entries
            .map { list ->
                val recent =
                    list
                        .filter { it.weight != null && it.calories != null }
                        .sortedBy { it.date }
                        .takeLast(14)
                if (recent.size < 2) return@map null

                val first = recent.first()
                val last = recent.last()

                val daysBetween = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()
                if (daysBetween == 0.0) return@map null

                val weightDelta = (last.weight ?: return@map null) - (first.weight ?: return@map null)
                val kcalDelta = (weightDelta * 7700) / daysBetween

                val avgCalories = recent.mapNotNull { it.calories }.average()
                (avgCalories - kcalDelta).toInt()
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null,
            )

    val goalCalories: StateFlow<Int?> =
        combine(
            goal,
            estimatedMaintenanceCalories,
            entries,
            sevenDayAvgWeight,
        ) { goal, maintenance, entriesList, avgWeight ->
            if (goal == null) return@combine null
            Log.d("GoalDebug", "goal=$goal, maintenance=$maintenance, entries=${entriesList.size}, avgWeight=$avgWeight")
            val currentWeight = avgWeight ?: entriesList.lastOrNull()?.weight ?: 0.0
            val rate = reconstructRateKgPerWeek(goal, currentWeight)

            val estimatedGoalDate =
                GoalCalculations.estimateGoalDate(
                    currentWeight = currentWeight,
                    goalWeight = goal.goalWeight,
                    rateKgPerWeek = rate,
                    timeMode = goal.timeMode,
                    goalType = goal.type,
                )

            val (total, dailyDelta) =
                estimateCalories(
                    goalType = goal.type,
                    currentWeight = currentWeight,
                    goalWeight = goal.goalWeight,
                    durationWeeks = goal.durationWeeks,
                    estimatedGoalDate = estimatedGoalDate,
                    targetDate = goal.targetDate?.let { Date(it) },
                    rateKgPerWeek = rate,
                    timeMode = goal.timeMode,
                )

            val result = maintenance?.plus((dailyDelta ?: 0))
            Log.d("GoalDebug", "goalCalories: total=$total, dailyDelta=$dailyDelta, result=$result")
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Helper for calculating the user's recent rate of weight change (kg/week)
    private fun estimateActualRateKgPerWeek(entries: List<WeightEntry>): Double {
        val recent =
            entries
                .filter { it.weight != null }
                .sortedBy { it.date }
                .takeLast(14)
        if (recent.size < 2) return 0.0
        val first = recent.first()
        val last = recent.last()
        val daysBetween = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()
        if (daysBetween == 0.0) return 0.0
        return ((last.weight ?: 0.0) - (first.weight ?: 0.0)) / daysBetween * 7.0
    }

    // Your main combined flow for progress:
    val goalProgress: StateFlow<GoalProgress?> =
        combine(
            goal,
            estimatedMaintenanceCalories,
            entries,
            sevenDayAvgWeight,
        ) { goal, maintenanceCalories, entriesList, avgWeight ->
            if (goal == null || maintenanceCalories == null || entriesList.isEmpty()) return@combine null

            // Get the current weight (average preferred)
            val currentWeight = avgWeight ?: entriesList.lastOrNull()?.weight ?: 0.0

            // Get the user's goal weight, or fallback to currentWeight
            val goalWeight = goal.goalWeight ?: currentWeight

            // Calculate actual recent trend
            val actualRateKgPerWeek = estimateActualRateKgPerWeek(entriesList)

            // User's intended rate (from UI or goal settings)
            val targetRateKgPerWeek = reconstructRateKgPerWeek(goal, currentWeight)

            // Estimate date you will reach goal at current trend
            val estimatedGoalDate: Date? =
                GoalCalculations.estimateGoalDate(
                    currentWeight = currentWeight,
                    goalWeight = goalWeight,
                    rateKgPerWeek = targetRateKgPerWeek,
                    timeMode = goal.timeMode,
                    goalType = goal.type,
                )

            // Get the target date (nullable)
            val targetDate: Date? = goal.targetDate?.let { Date(it) }

            // Estimate needed daily calories to hit the goal "on time"
            val (_, dailyCalories) =
                estimateCalories(
                    currentWeight = currentWeight,
                    goalWeight = goalWeight,
                    durationWeeks = goal.durationWeeks,
                    estimatedGoalDate = targetDate, // used only in BY_RATE mode
                    targetDate = targetDate,
                    rateKgPerWeek = targetRateKgPerWeek,
                    timeMode = goal.timeMode,
                    goalType = goal.type,
                )

            //  Final target intake: if ahead of schedule, maintain!
            val isAheadOfSchedule =
                estimatedGoalDate != null &&
                    targetDate != null &&
                    estimatedGoalDate.before(targetDate)

            val targetCalories =
                if (isAheadOfSchedule || estimatedGoalDate == null) {
                    maintenanceCalories
                } else {
                    maintenanceCalories + (dailyCalories ?: 0)
                }
            Log.d(
                "GoalProgress",
                "isAheadOfSchedule=$isAheadOfSchedule, estimatedGoalDate=$estimatedGoalDate, targetDate=$targetDate, dailyCalories=$dailyCalories",
            )

            GoalProgress(
                targetCalories = targetCalories,
                estimatedGoalDate = estimatedGoalDate,
                targetDate = targetDate,
                isAheadOfSchedule = isAheadOfSchedule,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress

    fun syncHealthConnect() {
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val weightRecords =
                    healthConnectClient
                        .readRecords<WeightRecord>(
                            ReadRecordsRequest(
                                recordType = WeightRecord::class,
                                timeRangeFilter =
                                    TimeRangeFilter.between(
                                        startTime = Instant.now().minus(30, ChronoUnit.DAYS),
                                        endTime = Instant.now(),
                                    ),
                            ),
                        ).records

                Log.d("WeightViewModel", "Found ${weightRecords.size} weight records")
                val weightByDate: Map<LocalDate, Double> =
                    weightRecords
                        .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
                        .mapValues { entry ->
                            entry.value
                                .maxByOrNull { it.time }
                                ?.weight
                                ?.inKilograms ?: 0.0
                        }

                val nutritionRecords =
                    healthConnectClient
                        .readRecords<NutritionRecord>(
                            ReadRecordsRequest(
                                recordType = NutritionRecord::class,
                                timeRangeFilter =
                                    TimeRangeFilter.between(
                                        startTime = Instant.now().minus(30, ChronoUnit.DAYS),
                                        endTime = Instant.now(),
                                    ),
                            ),
                        ).records
                Log.d("WeightViewModel", "Found ${nutritionRecords.size} nutrition records")

                val caloriesByDate: Map<LocalDate, Int> =
                    nutritionRecords
                        .groupBy { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
                        .mapValues { entry ->
                            entry.value
                                .sumOf { it.energy?.inKilocalories?.toInt() ?: 0 }
                        }

                val allDates: Set<LocalDate> = weightByDate.keys + caloriesByDate.keys

                val entries =
                    allDates.map { date ->
                        Log.d(
                            "WeightViewModel",
                            "Syncing date: $date, weight: ${weightByDate[date]}, calories: ${caloriesByDate[date]}",
                        )
                        WeightEntry(
                            date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            weight = weightByDate[date],
                            calories = caloriesByDate[date],
                        )
                    }

                dao?.insertAll(entries)
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error syncing with Health Connect", e)
            } finally {
                _syncInProgress.value = false
            }
        }
    }

    val dao: WeightDao? get() = if (BuildConfig.DEBUG) repository.weightDao else null
}
