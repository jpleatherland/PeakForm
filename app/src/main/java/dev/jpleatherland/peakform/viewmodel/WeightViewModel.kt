package dev.jpleatherland.peakform.viewmodel

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jpleatherland.peakform.BuildConfig
import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalDao
import dev.jpleatherland.peakform.data.GoalManager
import dev.jpleatherland.peakform.data.GoalProgress
import dev.jpleatherland.peakform.data.GoalRepository
import dev.jpleatherland.peakform.data.GoalSegment
import dev.jpleatherland.peakform.data.GoalSegmentRepository
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.data.WeightDao
import dev.jpleatherland.peakform.data.WeightEntry
import dev.jpleatherland.peakform.data.WeightRepository
import dev.jpleatherland.peakform.util.GoalCalculations
import dev.jpleatherland.peakform.util.GoalProjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val goalSegmentRepository: GoalSegmentRepository,
    private val healthConnectClient: HealthConnectClient,
) : ViewModel() {
    val entries: StateFlow<List<WeightEntry>> =
        repository
            .getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entriesAsc: StateFlow<List<WeightEntry>> =
        repository
            .getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<Goal?> =
        goalRepository
            .getLatestGoal()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val estimatedMaintenanceCalories: StateFlow<Int?> =
        entries
            .map { list ->
                val now = System.currentTimeMillis()
                val fourteenDaysAgo = now - TimeUnit.DAYS.toMillis(14)
                val recent =
                    list
                        .filter { it.weight != null && it.calories != null && it.date >= fourteenDaysAgo }
                        .sortedBy { it.date }
                if (recent.size < 2) return@map null

                val first = recent.first()
                val last = recent.last()

                val daysBetween = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()
                if (daysBetween < 7) return@map null // Must cover at least a week

                val weightDelta = (last.weight ?: return@map null) - (first.weight ?: return@map null)
                val kcalDelta = (weightDelta * 7700) / daysBetween

                val avgCalories = recent.mapNotNull { it.calories }.average()
                val estimate = (avgCalories - kcalDelta).toInt()

                if (estimate in 1000..6000) estimate else null // Clamp out crazy values
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // === NEW: Centralized Projection State ===
    val goalProjection: StateFlow<GoalProjection?> =
        combine(
            goal,
            estimatedMaintenanceCalories,
            entries,
            sevenDayAvgWeight,
        ) { goal, maintenance, entryList, avgWeight ->
            val currentWeight = avgWeight ?: entryList.lastOrNull()?.weight ?: 70.0

            val rateInput =
                when (goal?.rateMode) {
                    RateMode.KG_PER_WEEK -> goal.ratePerWeek?.toString() ?: ""
                    RateMode.BODYWEIGHT_PERCENT -> goal.ratePercent?.toString() ?: ""
                    RateMode.PRESET -> "" // not used
                    else -> ""
                }

            GoalCalculations.project(
                goal = goal,
                currentWeight = currentWeight,
                avgMaintenance = maintenance,
                rateInput = rateInput,
                selectedPreset = goal?.ratePreset,
                durationWeeks = goal?.durationWeeks,
                targetDate = goal?.targetDate?.let { Date(it) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Example: derived flows for compatibility
    val goalCalories: StateFlow<Int?> =
        goalProjection
            .map { it?.targetCalories }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val goalProgress: StateFlow<GoalProgress?> =
        combine(goalProjection, goal) { projection, goal ->
            if (projection == null || goal == null) return@combine null
            GoalProgress(
                targetCalories = projection.targetCalories ?: 0,
                estimatedGoalDate = projection.goalDate,
                targetDate = goal.targetDate?.let { Date(it) },
                isAheadOfSchedule =
                    projection.goalDate?.let { estimated ->
                        goal.targetDate?.let { target -> estimated.before(Date(target)) }
                    } ?: false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addEntry(
        weight: Double?,
        calories: Int?,
        date: Long,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                Log.d("Debug", "App DB instance: ${dao.hashCode()}")
                val entry = WeightEntry(weight = weight, calories = calories, date = date)
                val id = repository.insert(entry)
                onResult(id > 0)
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error adding entry", e)
                onResult(false)
            }
        }
    }

    fun updateEntry(entry: WeightEntry) {
        viewModelScope.launch { repository.updateEntry(entry) }
    }

    fun addEntries(entries: List<WeightEntry>) {
        viewModelScope.launch { repository.insertAll(entries) }
    }

    fun setGoal(goal: Goal) {
        viewModelScope.launch { goalRepository.insert(goal) }
    }

    fun clearGoal() {
        viewModelScope.launch { goalRepository.clearGoal() }
    }

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
                            entry.value.sumOf { it.energy?.inKilocalories?.toInt() ?: 0 }
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

                addEntries(entries)
            } catch (e: Exception) {
                Log.e("WeightViewModel", "Error syncing with Health Connect", e)
            } finally {
                _syncInProgress.value = false
            }
        }
    }

    private val goalManager = GoalManager(repository, goalRepository, goalSegmentRepository, estimatedMaintenanceCalories)

    init {
        goalManager.observeWeightAndAdjustSegments(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val goalSegments: StateFlow<List<GoalSegment>> =
        goal
            .flatMapLatest { goalOrNull ->
                goalOrNull?.let { goalSegmentRepository.getAllSegmentsForGoal(it.id) }
                    ?: flowOf(emptyList())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dao: WeightDao? get() = if (BuildConfig.DEBUG) repository.weightDao else null
    val goalDao: GoalDao? get() = if (BuildConfig.DEBUG) goalRepository.dao else null

    fun deleteWeightEntry(entry: WeightEntry) =
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }

    suspend fun resetAllData() {
        goalRepository.clearGoal()
        goalSegmentRepository.clearAllGoalSegments()
        repository.deleteAllEntries()
    }
}
