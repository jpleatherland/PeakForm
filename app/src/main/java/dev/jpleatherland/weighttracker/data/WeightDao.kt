package dev.jpleatherland.weighttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(entry: WeightEntry)

    @Query("SELECT * FROM WeightEntry ORDER BY date DESC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM WeightEntry WHERE date >= :since ORDER BY date DESC")
    fun getEntriesSince(since: Long): Flow<List<WeightEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntry>)

    @Query("DELETE FROM WeightEntry")
    suspend fun deleteAll()

    @Update
    suspend fun update(entry: WeightEntry)
}
