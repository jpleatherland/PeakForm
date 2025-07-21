package dev.jpleatherland.weighttracker.util

import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalSegment
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.data.GoalType
import dev.jpleatherland.weighttracker.data.RateMode
import dev.jpleatherland.weighttracker.data.RatePreset
import dev.jpleatherland.weighttracker.util.asDayEpochMillis
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.abs

data class GoalProjection(
    val rateKgPerWeek: Double, // signed (+/-) according to direction
    val goalDate: Date?, // calculated or set target date
    val finalWeight: Double?, // projected weight at goal or duration end
    val weightChange: Double?, // total expected change
    val totalCalories: Int?, // total kcals to lose/gain over period
    val dailyCalories: Int?, // required daily surplus/deficit for rate
    val targetCalories: Int?, // maintenance + dailyCalories
)

object GoalCalculations {
    /**
     * The one function to rule them all: returns everything you need for any screen.
     */
    fun project(
        goal: Goal?, // nullable for "no goal" case
        currentWeight: Double,
        avgMaintenance: Int?,
        rateInput: String,
        selectedPreset: RatePreset? = null,
        durationWeeks: Int? = null,
        targetDate: Date? = null,
    ): GoalProjection {
        // If no goal is set, return a mostly-empty projection
        if (goal == null) {
            return GoalProjection(
                rateKgPerWeek = 0.0,
                goalDate = null,
                finalWeight = null,
                weightChange = null,
                totalCalories = null,
                dailyCalories = null,
                targetCalories = null,
            )
        }

        // --- 1. Determine direction (sign) ---
        val rateSign =
            goal.goalWeight?.let { gw ->
                when {
                    gw < currentWeight -> -1.0 // Cutting
                    gw > currentWeight -> 1.0 // Bulking
                    else -> 0.0
                }
            } ?: when (goal.type) {
                GoalType.CUT -> -1.0
                GoalType.BULK -> 1.0
                else -> 0.0
            }

        // --- 2. Calculate raw rate (absolute value) ---

        val rateKgPerWeek: Double =
            when (goal.timeMode) {
                GoalTimeMode.BY_DATE -> {
                    // Calculate how many weeks to go
                    val date = targetDate ?: goal.targetDate?.let { Date(it) }
                    if (goal.goalWeight != null && date != null) {
                        val millisDiff = date.time - System.currentTimeMillis()
                        val weeks = millisDiff / (1000.0 * 60 * 60 * 24 * 7)
                        if (weeks > 0) {
                            val delta = goal.goalWeight - currentWeight
                            // Negative for cut, positive for bulk (the sign is baked in delta)
                            delta / weeks
                        } else {
                            0.0
                        }
                    } else {
                        0.0
                    }
                }
                else -> {
                    val rawRate =
                        when (goal.rateMode) {
                            RateMode.KG_PER_WEEK -> rateInput.toDoubleOrNull() ?: 0.0
                            RateMode.BODYWEIGHT_PERCENT -> ((goal.ratePercent ?: rateInput.toDoubleOrNull() ?: 0.0) / 100.0) * currentWeight
                            RateMode.PRESET -> (goal.ratePreset ?: selectedPreset ?: RatePreset.LEAN).percentPerWeek * currentWeight
                            else -> 0.0
                        }
                    // Handle sign here if you want, e.g. for cut/bulk
                    rawRate * rateSign
                }
            }

        // --- 3. Calculate duration (weeks) ---
        val weeks: Double? =
            when (goal.timeMode) {
                GoalTimeMode.BY_RATE ->
                    if (goal.goalWeight != null && abs(rateKgPerWeek) > 0.0) {
                        abs(goal.goalWeight - currentWeight) / abs(rateKgPerWeek)
                    } else {
                        null
                    }
                GoalTimeMode.BY_DATE ->
                    targetDate?.let {
                        val millisDiff = it.time - System.currentTimeMillis()
                        millisDiff / (1000.0 * 60 * 60 * 24 * 7)
                    }
                GoalTimeMode.BY_DURATION ->
                    durationWeeks?.toDouble()
            }

        // --- 4. Calculate final/projected weight ---
        val finalWeight: Double? =
            when (goal.timeMode) {
                GoalTimeMode.BY_DURATION ->
                    durationWeeks?.let { currentWeight + (rateKgPerWeek * it) }
                GoalTimeMode.BY_DATE, GoalTimeMode.BY_RATE ->
                    goal.goalWeight
            }

        // --- 5. Calculate weight change (total) ---
        val weightChange: Double? = weeks?.let { rateKgPerWeek * it }

        // --- 6. Estimate goal date ---
        val goalDate =
            when (goal.timeMode) {
                GoalTimeMode.BY_RATE ->
                    if (goal.goalWeight != null && abs(rateKgPerWeek) > 0.0) {
                        Date(
                            System.currentTimeMillis() +
                                ((abs(goal.goalWeight - currentWeight) / abs(rateKgPerWeek)) * 7 * 24 * 60 * 60 * 1000).toLong(),
                        )
                    } else {
                        null
                    }
                GoalTimeMode.BY_DATE -> targetDate
                GoalTimeMode.BY_DURATION ->
                    durationWeeks?.let {
                        Date(System.currentTimeMillis() + (it * 7 * 24 * 60 * 60 * 1000).toLong())
                    }
            }

        // --- 7. Calorie calculations ---
        val totalCalories = weightChange?.let { (it * 7700).toInt() }
        val days = weeks?.let { (it * 7).toInt() }
        val dailyCalories =
            if (days != null && days > 0 && totalCalories != null) {
                totalCalories / days
            } else {
                null
            }
        val targetCalories = avgMaintenance?.plus(dailyCalories ?: 0)

        return GoalProjection(
            rateKgPerWeek = rateKgPerWeek,
            goalDate = goalDate,
            finalWeight = finalWeight,
            weightChange = weightChange,
            totalCalories = totalCalories,
            dailyCalories = dailyCalories,
            targetCalories = targetCalories,
        )
    }

    fun generateSegments(
        goal: Goal,
        currentWeight: Double,
        maintenanceCalories: Int,
        today: LocalDate = LocalDate.now()
    ): List<GoalSegment> {
        val projection = project(goal, currentWeight, maintenanceCalories, rateInput =
            when (goal.rateMode) {
                RateMode.KG_PER_WEEK -> goal.ratePerWeek?.toString() ?: ""
                RateMode.BODYWEIGHT_PERCENT -> goal?.ratePercent?.toString() ?: ""
                RateMode.PRESET -> "" // not used
                else -> ""
            })
        val goalId = goal.id ?: return emptyList()
        val ratePerWeek = projection.rateKgPerWeek.takeIf { it != 0.0 } ?: return emptyList()

        val goalDateLocal = projection.goalDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val totalWeeks = ChronoUnit.WEEKS.between(today, goalDateLocal).toInt().coerceAtLeast(1)

        val segments = mutableListOf<GoalSegment>()

        var startWeight = currentWeight
        var startDate = today

        repeat(totalWeeks) { i ->
            val endDate = startDate.plusWeeks(1)
            val endWeight = startWeight + ratePerWeek
            val entity = GoalSegment(
                goalId = goalId,
                startDate = startDate.toEpochMillis(),
                endDate = endDate.toEpochMillis(),
                startWeight = startWeight,
                endWeight = endWeight,
                targetCalories = projection.targetCalories ?: 0
            )
            segments += entity

            startWeight = endWeight
            startDate = endDate
        }

        return segments
    }

}
