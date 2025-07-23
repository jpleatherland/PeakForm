package dev.jpleatherland.weighttracker.dev

import android.util.Log
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalDao
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.data.GoalType
import dev.jpleatherland.weighttracker.data.RateMode
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.util.toEpochMillis
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

enum class TestDataType {
    TRENDING_UP,
    TRENDING_DOWN,
    RANDOM,
    BULK_WITH_JUMP,
    CUT_WITH_JUMP,
    BULK_WITH_JUMP_BY_DATE,
    CUT_WITH_JUMP_BY_DATE,
}

fun generateTestData(
    scope: CoroutineScope,
    dao: WeightDao,
    goalDao: GoalDao,
    type: TestDataType,
    viewModel: WeightViewModel,
) {
    scope.launch {
        val today = LocalDate.now()
        val startDate = today.minusDays(29)
        val startDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val goal =
            when (type) {
                TestDataType.TRENDING_DOWN, TestDataType.CUT_WITH_JUMP ->
                    Goal(
                        goalWeight = 62.0,
                        type = GoalType.CUT,
                        timeMode = GoalTimeMode.BY_RATE,
                        targetDate = null,
                        ratePerWeek = 0.5,
                        durationWeeks = null,
                        rateMode = RateMode.KG_PER_WEEK,
                        createdAt = startDateMillis,
                    )
                TestDataType.TRENDING_UP, TestDataType.BULK_WITH_JUMP ->
                    Goal(
                        goalWeight = 78.0,
                        type = GoalType.BULK,
                        timeMode = GoalTimeMode.BY_RATE,
                        targetDate = null,
                        ratePerWeek = 0.3,
                        durationWeeks = null,
                        rateMode = RateMode.KG_PER_WEEK,
                        createdAt = startDateMillis,
                    )
                TestDataType.RANDOM ->
                    Goal(
                        goalWeight = 70.0,
                        type = GoalType.TARGET_WEIGHT,
                        timeMode = GoalTimeMode.BY_DATE,
                        targetDate = System.currentTimeMillis() + 14 * 24 * 3600 * 1000,
                        ratePerWeek = null,
                        durationWeeks = null,
                        rateMode = null,
                        createdAt = startDateMillis,
                    )
                TestDataType.BULK_WITH_JUMP_BY_DATE ->
                    Goal(
                        goalWeight = 80.0,
                        type = GoalType.BULK,
                        timeMode = GoalTimeMode.BY_DATE,
                        targetDate = startDate.plusDays(60).toEpochMillis(),
                        ratePerWeek = null,
                        durationWeeks = null,
                        rateMode = null,
                        createdAt = startDateMillis,
                    )
                TestDataType.CUT_WITH_JUMP_BY_DATE ->
                    Goal(
                        goalWeight = 62.0,
                        type = GoalType.CUT,
                        timeMode = GoalTimeMode.BY_DATE,
                        targetDate = startDate.plusDays(60).toEpochMillis(),
                        ratePerWeek = null,
                        durationWeeks = null,
                        rateMode = null,
                        createdAt = startDateMillis,
                    )
            }
        goalDao.insert(
            goal,
        )
        val window = 14 // rolling window, like your app
        val desiredMaintenance =
            when (type) {
                TestDataType.TRENDING_UP, TestDataType.BULK_WITH_JUMP, TestDataType.BULK_WITH_JUMP_BY_DATE -> 2500
                TestDataType.TRENDING_DOWN, TestDataType.CUT_WITH_JUMP, TestDataType.CUT_WITH_JUMP_BY_DATE -> 2000
                else -> 2200
            }

        val weights =
            (0 until 30).map { i ->
                when (type) {
                    TestDataType.TRENDING_UP -> 68.0 + (i * 0.042857)
                    TestDataType.TRENDING_DOWN -> 72.0 - (i * 0.07143)
                    TestDataType.BULK_WITH_JUMP, TestDataType.BULK_WITH_JUMP_BY_DATE -> {
                        if (i < 15) {
                            68.0 + (i * 0.042857)
                        } else {
                            val weightAt15 = 68.0 + (15 * 0.042857)
                            weightAt15 + ((i - 15) * 0.12)
                        }
                    }
                    TestDataType.CUT_WITH_JUMP, TestDataType.CUT_WITH_JUMP_BY_DATE -> {
                        if (i < 15) {
                            72.0 - (i * 0.07143)
                        } else {
                            val weightAt15 = 72.0 - (15 * 0.07143)
                            weightAt15 // plateau
                        }
                    }
                    TestDataType.RANDOM -> 70.0 // (just to avoid randomness here)
                }
            }

        val entries =
            (0 until 30).map { i ->
                val date = startDate.plusDays(i.toLong())
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val weight = weights[i]
                // Rolling window
                val windowStart = maxOf(0, i - window + 1)
                val daysInWindow = i - windowStart
                val weightDelta =
                    if (daysInWindow > 0) {
                        weights[i] - weights[windowStart]
                    } else {
                        0.0
                    }
                val extraKcal = if (daysInWindow > 0) (weightDelta * 7700 / daysInWindow) else 0.0
                val calories = (desiredMaintenance + extraKcal + Random.nextInt(-50, 51)).toInt()

                WeightEntry(date = dateMillis, weight = weight, calories = calories)
            }

        for ((i, entry) in entries.withIndex()) {
            viewModel.addEntry(entry.weight, entry.calories, entry.date) {}
            Log.d("TestDataUtil", "Inserting entry $i of ${entries.size}")
            dao.insert(entry)
            delay(50)
        }
    }
}
