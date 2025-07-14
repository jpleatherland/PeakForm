package dev.jpleatherland.weighttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.data.WeightRepository
import dev.jpleatherland.weighttracker.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeightViewModel(
    private val repository: WeightRepository
) : ViewModel() {
    val entries: StateFlow<List<WeightEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(weight: Double?, calories: Int?) {
        viewModelScope.launch {
            val entry = WeightEntry(weight = weight, calories = calories)
            repository.insert(entry)
        }
    }

    val dao: WeightDao? get() = if (BuildConfig.DEBUG) repository.weightDao else null
}
