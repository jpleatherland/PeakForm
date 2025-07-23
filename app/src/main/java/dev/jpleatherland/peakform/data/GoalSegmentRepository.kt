package dev.jpleatherland.peakform.data

import kotlinx.coroutines.flow.Flow

class GoalSegmentRepository(
    private val dao: GoalSegmentDao,
) {
    suspend fun insert(segment: GoalSegment): Long = dao.insert(segment)

    fun getAllSegmentsForGoal(goalId: Int): Flow<List<GoalSegment>> = dao.getAllSegmentsForGoal(goalId)

    suspend fun clearAllGoalSegments() = dao.clearAllGoalSegments()

//    fun getLatestSegmentForGoal(goalId: Int) = dao.getLatestSegmentForGoal(goalId)
}
