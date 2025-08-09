package dev.jpleatherland.peakform.util

import dev.jpleatherland.peakform.data.WeightEntry
import java.util.concurrent.TimeUnit

object MaintenanceCalculator {
    private const val KCAL_PER_KG = 7700.0
    private val DAY_MS = TimeUnit.DAYS.toMillis(1)

    fun estimate(entries: List<WeightEntry>): MaintenanceEstimateResult {
        val now = System.currentTimeMillis()
        val fourteenDaysAgo = now - TimeUnit.DAYS.toMillis(14)

        // Use only days in the last 14 with both weight and calories
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
        val spanDays = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()

        if (spanDays < 7.0) {
            return MaintenanceEstimateResult(
                estimatedCalories = null,
                message = "To see your estimated maintenance calories your recent entries must cover at least 7 days for a maintenance estimate.",
                entryCount = recent.size,
            )
        }

        // Build (x,y) pairs where x = days since first entry, y = weight
        val xy =
            recent.mapNotNull { e ->
                val w = e.weight ?: return@mapNotNull null
                val tDays = (e.date - first.date).toDouble() / DAY_MS.toDouble()
                tDays to w.toDouble()
            }

        // Need at least 3 points for a stable slope; otherwise fall back to endpoints
        val slopeKgPerDay =
            if (xy.size >= 3) {
                leastSquaresSlope(xy)
            } else {
                // endpoint fallback
                val wDelta = (last.weight!! - first.weight!!)
                wDelta / spanDays
            }

        // Convert slope to kcal/day (negative slope means weight is dropping)
        val kcalPerDayFromTrend = slopeKgPerDay * KCAL_PER_KG

        // Average calories eaten over the same window
        val avgCalories = recent.mapNotNull { it.calories }.map { it.toDouble() }.average()

        // Maintenance = what you ate minus the daily deficit/surplus implied by the weight trend
        val estimate = (avgCalories - kcalPerDayFromTrend).toInt()

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

    // Ordinary Least Squares slope for y ~ a + b*x
    private fun leastSquaresSlope(xy: List<Pair<Double, Double>>): Double {
        val n = xy.size
        val xMean = xy.sumOf { it.first } / n
        val yMean = xy.sumOf { it.second } / n

        var num = 0.0
        var den = 0.0
        for ((x, y) in xy) {
            val dx = x - xMean
            num += dx * (y - yMean)
            den += dx * dx
        }
        // If all x are identical (shouldn’t happen with time) avoid div-by-zero
        return if (den == 0.0) 0.0 else num / den
    }
}

data class MaintenanceEstimateResult(
    val estimatedCalories: Int?,
    val message: String?, // null if valid
    val entryCount: Int,
)
