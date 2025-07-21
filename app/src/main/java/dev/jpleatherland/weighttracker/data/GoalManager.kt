package dev.jpleatherland.weighttracker.data

import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.util.GoalCalculations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch

class GoalManager(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val goalSegmentRepository: GoalSegmentRepository,
    private val maintenanceCalories: StateFlow<Int?>,
) {
    fun observeWeightAndAdjustSegments(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            combine(
                weightRepository.getAllEntries(),
                goalRepository.getLatestGoal(),
                maintenanceCalories,
            ) { entries, goal, maintenance ->
                Triple(entries, goal, maintenance)
            }.collect { (entries, goal, maintenance) ->
                if (goal == null || entries.isEmpty() || maintenance == null) return@collect

                val segments =
                    goalSegmentRepository
                        .getAllSegmentsForGoal(goal.id)
                val activeSegment = segments.lastOrNull() ?: return@collect
                val avgWeight = entries.mapNotNull { it.weight }.averageOrNull() ?: return@collect
                val recentEntries = entries.takeLast(14)
                val avgCalories = recentEntries.mapNotNull { it.calories }.averageIntOrNull() ?: return@collect

                val newSegment = GoalCalculations.generateSegment(goal, entries, maintenance)

                newSegment?.let {
                    goalSegmentRepository.insert(newSegment)
                }
            }
        }
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun List<Int>.averageIntOrNull(): Double? = if (isEmpty()) null else average().toDouble()
}
