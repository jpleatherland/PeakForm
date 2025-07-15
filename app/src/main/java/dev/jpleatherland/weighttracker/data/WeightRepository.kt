package dev.jpleatherland.weighttracker.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(
    private val dao: WeightDao,
) {
    suspend fun insert(entry: WeightEntry) {
        dao.insert(entry)
    }

    fun getAllEntries(): Flow<List<WeightEntry>> = dao.getAllEntries()

    fun getEntriesSince(since: Long): Flow<List<WeightEntry>> = dao.getEntriesSince(since)

    suspend fun updateEntry(entry: WeightEntry) {
        dao.update(entry)
    }

    val weightDao: WeightDao get() = dao
}
