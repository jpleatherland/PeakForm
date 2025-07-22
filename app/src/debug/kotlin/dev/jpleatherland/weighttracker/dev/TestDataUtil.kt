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
            }
        goalDao.insert(
            goal,
        )

        val entries =
            (0 until 30).map { i ->
                val date = startDate.plusDays(i.toLong())
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val weight =
                    when (type) {
                        TestDataType.TRENDING_UP -> 68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                        TestDataType.TRENDING_DOWN -> 72.0 - (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                        TestDataType.BULK_WITH_JUMP -> {
                            // Steady increase, then a big drop
                            if (i < 15) {
                                68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                            } else {
                                61.0 + ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                            }
                        }
                        TestDataType.CUT_WITH_JUMP -> {
                            // Steady decrease, then a big jump up
                            if (i < 15) {
                                72.0 - (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                            } else {
                                78.0 - ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                            }
                        }
                        TestDataType.RANDOM -> 70.0 + Random.nextDouble(-1.5, 1.5)
                        TestDataType.BULK_WITH_JUMP_BY_DATE -> {
                            // Steady increase, then a big drop
                            if (i < 15) {
                                68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                            } else {
                                61.0 + ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                            }
                        }
                    }
                val calories = 2500 + Random.nextInt(-400, 400)
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
