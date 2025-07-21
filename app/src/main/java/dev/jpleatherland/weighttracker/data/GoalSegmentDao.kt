package dev.jpleatherland.weighttracker.data

import androidx.room.*

@Dao
interface GoalSegmentDao {
    @Query("SELECT * FROM goal_segment WHERE goalId = :goalId ORDER BY startDate")
    suspend fun getAllSegmentsForGoal(goalId: Int): List<GoalSegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: GoalSegment): Long

    @Query("DELETE FROM goal_segment WHERE goalId = :goalId")
    suspend fun clearSegmentsForGoal(goalId: Int)
}
