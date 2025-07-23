package dev.jpleatherland.peakform.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(
    private val dao: WeightDao,
) {
    suspend fun insert(entry: WeightEntry): Long = dao.insert(entry)

    suspend fun insertAll(entries: List<WeightEntry>) = dao.insertAll(entries)

    fun getAllEntries(): Flow<List<WeightEntry>> = dao.getAllEntries()

    fun getEntriesSince(since: Long): Flow<List<WeightEntry>> = dao.getEntriesSince(since)

    suspend fun updateEntry(entry: WeightEntry) {
        dao.update(entry)
    }

    suspend fun deleteEntry(entry: WeightEntry) = dao.deleteById(entry.id)

    suspend fun deleteAllEntries() = dao.deleteAll()

    val weightDao: WeightDao get() = dao
}
