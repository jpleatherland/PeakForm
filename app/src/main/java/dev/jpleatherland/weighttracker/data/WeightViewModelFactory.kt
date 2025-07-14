package dev.jpleatherland.weighttracker.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

class WeightViewModelFactory(
    private val repository: WeightRepository
): ViewModelProvider.Factory {
    override fun <T: ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeightViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeightViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}