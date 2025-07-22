package dev.jpleatherland.weighttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalSegmentDao {
    @Query("SELECT * FROM goal_segment WHERE goalId = :goalId ORDER BY startDate")
    fun getAllSegmentsForGoal(goalId: Int): Flow<List<GoalSegment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: GoalSegment): Long

    @Query("DELETE FROM goal_segment WHERE goalId = :goalId")
    suspend fun clearSegmentsForGoal(goalId: Int)

    @Query("DELETE FROM goal_segment")
    suspend fun clearAllGoalSegments()
}
