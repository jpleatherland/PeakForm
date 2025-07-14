package com.example.weighttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weighttracker.data.WeightEntry
import com.example.weighttracker.data.WeightRepository
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
}
