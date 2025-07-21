package dev.jpleatherland.weighttracker.data

import android.util.Log
import dev.jpleatherland.weighttracker.data.Goal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch

class GoalManager(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val goalSegmentRepository: GoalSegmentRepository,
) {
    fun observeWeightAndAdjustSegments(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            combine(
                weightRepository.getAllEntries(),
                goalRepository.getLatestGoal(),
            ) { entries, goal ->
                Pair(entries, goal)
            }.collect { (entries, goal) ->
                Log.d("GoalManager", "Observing weight entries and goal: $goal with ${entries.size} entries")
                if (goal == null || entries.isEmpty()) return@collect

                val segments =
                    goalSegmentRepository
                        .getAllSegmentsForGoal(goal.id)
                val activeSegment = segments.lastOrNull() ?: return@collect

                // Logic to adjust segments based on weight entries
                // This is where you would implement the logic to update segments
            }
        }
    }
}
