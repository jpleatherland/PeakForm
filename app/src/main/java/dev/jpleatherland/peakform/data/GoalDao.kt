package dev.jpleatherland.peakform.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal ORDER BY createdAt DESC LIMIT 1")
    fun getLatestGoal(): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Query("DELETE FROM goal")
    suspend fun clearGoal()
}
