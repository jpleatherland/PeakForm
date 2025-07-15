package dev.jpleatherland.weighttracker.data

class GoalRepository(
    private val goalDao: GoalDao,
) {
    fun getLatestGoal() = goalDao.getLatestGoal()

    suspend fun insert(goal: Goal) {
        goalDao.insert(goal)
    }

    suspend fun clearGoal() {
        goalDao.clearGoal()
    }
}
