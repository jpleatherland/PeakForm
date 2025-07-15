package dev.jpleatherland.weighttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jpleatherland.weighttracker.BuildConfig
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalRepository
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.data.WeightRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WeightViewModel(
    private val repository: WeightRepository,
    private val goalRepository: GoalRepository,
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

    val dao: WeightDao? get() = if (BuildConfig.DEBUG) repository.weightDao else null
}
