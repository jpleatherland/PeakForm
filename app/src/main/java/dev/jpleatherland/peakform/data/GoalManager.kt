package dev.jpleatherland.peakform.data

import android.util.Log
import dev.jpleatherland.peakform.util.GoalCalculations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GoalManager(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val goalSegmentRepository: GoalSegmentRepository,
    private val maintenanceCalories: StateFlow<Int?>,
) {
    fun observeWeightAndAdjustSegments(scope: CoroutineScope) {
        scope.launch {
            combine(
                weightRepository.getAllEntries(),
                maintenanceCalories,
            ) { entries, maintenance ->
                Pair(entries, maintenance)
            }.collect { (entries, maintenance) ->
                val goal = goalRepository.getLatestGoal().value
                goal?.let {
                    val newSegment = GoalCalculations.maybeGenerateCorrectionSegment(it, entries, maintenance, goalSegmentRepository)
                    val lastSegment = goalSegmentRepository.getAllSegmentsForGoal(goal.id).firstOrNull()?.lastOrNull()

                    // Utility to compare segments:
                    fun segmentsAreEqual(
                        a: GoalSegment?,
                        b: GoalSegment?,
                    ): Boolean {
                        if (a == null || b == null) return false
                        return a.startDate == b.startDate &&
                            a.endDate == b.endDate &&
                            a.startWeight == b.startWeight &&
                            a.endWeight == b.endWeight
                    }

                    // Only insert if not a duplicate
                    if (newSegment != null && !segmentsAreEqual(newSegment, lastSegment)) {
                        Log.d("GoalManager", "Inserting corrective segment: $newSegment")
                        Log.d("GoalManager", "Last segment: $lastSegment Goal ID: ${goal.id}")
                        goalSegmentRepository.insert(newSegment)
                    } else {
                        Log.d("GoalManager", "Not inserting segment (duplicate or unnecessary)")
                    }
                }
            }
        }
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun List<Int>.averageIntOrNull(): Double? = if (isEmpty()) null else average().toDouble()
}
