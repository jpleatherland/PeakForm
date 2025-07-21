package dev.jpleatherland.weighttracker.data

class GoalSegmentRepository(
    private val dao: GoalSegmentDao,
) {
    suspend fun insert(segment: GoalSegment): Long = dao.insert(segment)

    suspend fun getAllSegmentsForGoal(goalId: Int) = dao.getAllSegmentsForGoal(goalId)

//    fun getLatestSegmentForGoal(goalId: Int) = dao.getLatestSegmentForGoal(goalId)
}
