package dev.jpleatherland.peakform.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GoalRepository(
    private val goalDao: GoalDao,
) {
    private val latestGoal: StateFlow<Goal?> =
        goalDao.getLatestGoal().stateIn(
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            SharingStarted.WhileSubscribed(5000),
            null,
        )

    fun getLatestGoal(): StateFlow<Goal?> = latestGoal

    suspend fun insert(goal: Goal) {
        goalDao.insert(goal)
    }

    suspend fun clearGoal() {
        goalDao.clearGoal()
    }

    val dao: GoalDao get() = goalDao
}
