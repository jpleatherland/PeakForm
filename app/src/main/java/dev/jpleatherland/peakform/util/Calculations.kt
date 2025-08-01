package dev.jpleatherland.peakform.util

import dev.jpleatherland.peakform.data.WeightEntry
import java.util.concurrent.TimeUnit

object MaintenanceCalculator {
    fun estimate(entries: List<WeightEntry>): MaintenanceEstimateResult {
        val now = System.currentTimeMillis()
        val fourteenDaysAgo = now - TimeUnit.DAYS.toMillis(14)

        val recent =
            entries
                .filter { it.weight != null && it.calories != null && it.date >= fourteenDaysAgo }
                .sortedBy { it.date }

        if (recent.size < 2) {
            return MaintenanceEstimateResult(
                estimatedCalories = null,
                message = "To see your estimated maintenance calories log at least two days of weight and calories in the last 14 days.",
                entryCount = recent.size,
            )
        }

        val first = recent.first()
        val last = recent.last()
        val daysBetween = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()

        if (daysBetween < 7) {
            return MaintenanceEstimateResult(
                estimatedCalories = null,
                message = "To see your estimated maintenance calories your recent entries must cover at least 7 days for a maintenance estimate.",
                entryCount = recent.size,
            )
        }

        val weightDelta =
            (last.weight ?: return MaintenanceEstimateResult(null, "Invalid weight data", recent.size)) -
                (first.weight ?: return MaintenanceEstimateResult(null, "Invalid weight data", recent.size))

        val kcalDelta = (weightDelta * 7700) / daysBetween
        val avgCalories = recent.mapNotNull { it.calories }.average()
        val estimate = (avgCalories - kcalDelta).toInt()

        return if (estimate in 1000..6000) {
            MaintenanceEstimateResult(estimate, null, recent.size)
        } else {
            MaintenanceEstimateResult(
                null,
                "Unable to calculate a reasonable maintenance value (check for very high/low calorie entries).",
                recent.size,
            )
        }
    }
}

data class MaintenanceEstimateResult(
    val estimatedCalories: Int?,
    val message: String?, // null if valid
    val entryCount: Int,
)
