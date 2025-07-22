package dev.jpleatherland.weighttracker.data

import kotlinx.coroutines.flow.Flow

class GoalSegmentRepository(
    private val dao: GoalSegmentDao,
) {
    suspend fun insert(segment: GoalSegment): Long = dao.insert(segment)

    fun getAllSegmentsForGoal(goalId: Int): Flow<List<GoalSegment>> = dao.getAllSegmentsForGoal(goalId)

//    fun getLatestSegmentForGoal(goalId: Int) = dao.getLatestSegmentForGoal(goalId)
}
