package dev.jpleatherland.weighttracker.data

import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

class WeightViewModelFactory(
    private val repository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val goalSegmentRepository: GoalSegmentRepository,
    private val healthConnectClient: HealthConnectClient,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeightViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeightViewModel(repository, goalRepository, goalSegmentRepository, healthConnectClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}
