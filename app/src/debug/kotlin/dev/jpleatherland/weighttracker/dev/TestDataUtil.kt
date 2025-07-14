package dev.jpleatherland.weighttracker.dev

import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.data.WeightEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

enum class TestDataType {
    TRENDING_UP,
    TRENDING_DOWN,
    RANDOM
}

fun generateTestData(scope: CoroutineScope, dao: WeightDao, type: TestDataType) {
    scope.launch {
        val today = LocalDate.now()
        val entries = (0 until 30).map { i ->
            val dateMillis = today.minusDays(i.toLong())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val weight = when (type) {
                TestDataType.TRENDING_DOWN -> 68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                TestDataType.TRENDING_UP -> 72.0 - (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                TestDataType.RANDOM -> 70.0 + Random.nextDouble(-1.5, 1.5)
            }

            val calories = 2500 + Random.nextInt(-400, 400)

            WeightEntry(date = dateMillis, weight = weight, calories = calories)
        }

        dao.insertAll(entries)
    }
}

fun wipeAllData(scope: CoroutineScope, dao: WeightDao) {
    scope.launch {
        dao.deleteAll()
    }
}
