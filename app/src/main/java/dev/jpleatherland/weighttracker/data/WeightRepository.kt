package dev.jpleatherland.weighttracker.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightDao) {
    suspend fun insert(entry: WeightEntry) {
        dao.insert(entry)
    }

    fun getAllEntries(): Flow<List<WeightEntry>> {
        return dao.getAllEntries()
    }

    fun getEntriesSince(since: Long): Flow<List<WeightEntry>> {
        return dao.getEntriesSince(since)
    }

    val weightDao: WeightDao get() = dao
}