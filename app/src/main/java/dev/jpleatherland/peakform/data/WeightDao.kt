package dev.jpleatherland.peakform.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntry): Long

    @Query("SELECT * FROM WeightEntry ORDER BY date DESC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM WeightEntry ORDER BY date ASC")
    fun getAllEntriesAscending(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM WeightEntry WHERE date >= :since ORDER BY date DESC")
    fun getEntriesSince(since: Long): Flow<List<WeightEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntry>)

    @Query("DELETE FROM WeightEntry")
    suspend fun deleteAll()

    @Query("DELETE FROM WeightEntry WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Update
    suspend fun update(entry: WeightEntry)
}
