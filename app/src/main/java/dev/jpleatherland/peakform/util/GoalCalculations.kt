package dev.jpleatherland.peakform.util

import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalSegment
import dev.jpleatherland.peakform.data.GoalSegmentRepository
import dev.jpleatherland.peakform.data.GoalTimeMode
import dev.jpleatherland.peakform.data.GoalType
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.data.RatePreset
import dev.jpleatherland.peakform.data.WeightEntry
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.absoluteValue

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
        goal: Goal?,
        currentWeight: Double,
        avgMaintenance: Int?,
        rateInput: String,
        selectedPreset: RatePreset? = null,
        durationWeeks: Int? = null, // this is user's input from UI
        targetDate: Date? = null, // this is user's input from UI
    ): GoalProjection {
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

        // 1. Direction (sign)
        val rateSign =
            goal.goalWeight?.let { gw ->
                when {
                    gw < currentWeight -> -1.0
                    gw > currentWeight -> 1.0
                    else -> 0.0
                }
            } ?: when (goal.type) {
                GoalType.CUT -> -1.0
                GoalType.BULK -> 1.0
                else -> 0.0
            }

        // 2. Raw rate
        val rateKgPerWeek: Double =
            when (goal.timeMode) {
                GoalTimeMode.BY_DATE -> {
                    // Calculate needed rate to hit target by target date
                    val date = targetDate ?: goal.targetDate?.let { Date(it) }
                    if (goal.goalWeight != null && date != null) {
                        val millisDiff = date.time - System.currentTimeMillis()
                        val weeks = millisDiff / (1000.0 * 60 * 60 * 24 * 7)
                        if (weeks > 0) {
                            val delta = goal.goalWeight - currentWeight
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
                    rawRate * rateSign
                }
            }

        // 3. Duration (weeks)
        val weeks: Double? =
            when (goal.timeMode) {
                GoalTimeMode.BY_RATE -> {
                    if (goal.goalWeight != null && abs(rateKgPerWeek) > 0.0) {
                        abs(goal.goalWeight - currentWeight) / abs(rateKgPerWeek)
                    } else {
                        null
                    }
                }
                GoalTimeMode.BY_DATE -> {
                    val date = targetDate ?: goal.targetDate?.let { Date(it) }
                    date?.let {
                        val millisDiff = it.time - System.currentTimeMillis()
                        val w = millisDiff / (1000.0 * 60 * 60 * 24 * 7)
                        if (w > 0) w else null
                    }
                }
                GoalTimeMode.BY_DURATION -> {
                    val d = durationWeeks ?: goal.durationWeeks
                    if (d != null && d > 0) d.toDouble() else null
                }
            }

        // 4. Final/projected weight
        val finalWeight: Double? =
            when (goal.timeMode) {
                GoalTimeMode.BY_DURATION -> {
                    val d = durationWeeks ?: goal.durationWeeks
                    if (d != null && d > 0) currentWeight + (rateKgPerWeek * d) else null
                }
                GoalTimeMode.BY_DATE, GoalTimeMode.BY_RATE -> goal.goalWeight
            }

        // 5. Weight change (total)
        val weightChange: Double? = weeks?.let { rateKgPerWeek * it }

        // 6. Goal date
        val goalDate: Date? =
            when (goal.timeMode) {
                GoalTimeMode.BY_RATE -> {
                    if (goal.goalWeight != null && abs(rateKgPerWeek) > 0.0) {
                        val durationWeeks = abs(goal.goalWeight - currentWeight) / abs(rateKgPerWeek)
                        Date(System.currentTimeMillis() + (durationWeeks * 7 * 24 * 60 * 60 * 1000).toLong())
                    } else {
                        null
                    }
                }
                GoalTimeMode.BY_DATE -> targetDate ?: goal.targetDate?.let { Date(it) }
                GoalTimeMode.BY_DURATION -> {
                    val d = durationWeeks ?: goal.durationWeeks
                    if (d != null && d > 0) Date(System.currentTimeMillis() + (d * 7 * 24 * 60 * 60 * 1000L)) else null
                }
            }

        // 7. Calorie calculations
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

    suspend fun maybeGenerateCorrectionSegment(
        goal: Goal,
        entries: List<WeightEntry>,
        estimatedMaintenance: Int?,
        goalSegmentRepository: GoalSegmentRepository,
    ): GoalSegment? {
        val goalId = goal.id ?: return null

        // 1. Get last correction segment, if any
        val lastSegment =
            goalSegmentRepository
                .getAllSegmentsForGoal(goalId)
                .firstOrNull()
                ?.lastOrNull()

        // 2. Find the base date and weight
        val baseDate: Long
        val baseWeight: Double
        if (lastSegment == null) {
            baseDate = entries.minByOrNull { it.date }?.date ?: return null
            baseWeight = entries.firstOrNull { it.weight != null }?.weight ?: return null
        } else {
            baseDate = lastSegment.endDate
            baseWeight = lastSegment.endWeight
        }

        // 3. Find all entries after the base date (with weight/calories)
        val recent =
            entries
                .filter { it.weight != null && it.calories != null && it.date > baseDate }
                .sortedBy { it.date }

        // --- Only proceed if there are at least 14 calendar days' data, and at least 7 days since last correction ---
        if (recent.size < 2) return null
        val first = recent.first()
        val last = recent.last()
        val daysBetween = TimeUnit.MILLISECONDS.toDays(last.date - first.date).toDouble()

        if (daysBetween < 14) return null // <--- Only act if you have 14 days' coverage
        if (TimeUnit.MILLISECONDS.toDays(last.date - baseDate) < 7) return null // <--- Only act if at least 7 days since last correction

        // Only use up to the last 14 entries within the last 14 days
        val fourteenDaysAgo = last.date - TimeUnit.DAYS.toMillis(14)
        val correctionWindow =
            recent
                .filter { it.date >= fourteenDaysAgo }
                .takeLast(14)
        if (correctionWindow.size < 2) return null
        val windowFirst = correctionWindow.first()
        val windowLast = correctionWindow.last()
        val windowDays = TimeUnit.MILLISECONDS.toDays(windowLast.date - windowFirst.date).toDouble()
        if (windowDays < 7) return null // Ensure 7 days minimum in the rolling window

        // 4. Calculate projection rate from baseWeight (not from just windowFirst)
        val projection =
            project(
                goal = goal,
                currentWeight = baseWeight,
                avgMaintenance = estimatedMaintenance,
                rateInput =
                    when (goal.rateMode) {
                        RateMode.KG_PER_WEEK -> goal.ratePerWeek?.toString() ?: ""
                        RateMode.BODYWEIGHT_PERCENT -> goal.ratePercent?.toString() ?: ""
                        RateMode.PRESET -> ""
                        else -> ""
                    },
                selectedPreset = goal.ratePreset,
                durationWeeks = goal.durationWeeks,
                targetDate = goal.targetDate?.let { Date(it) },
            )
        val projectedRate = projection.rateKgPerWeek
        if (projectedRate == 0.0) return null

        // 5. Compute actual rate in window
        val weightDelta = (windowLast.weight ?: return null) - (windowFirst.weight ?: return null)
        val actualRatePerWeek = (weightDelta / windowDays) * 7.0

        val percentDeviation = ((actualRatePerWeek - projectedRate) / projectedRate).absoluteValue
        if (percentDeviation <= 0.1) return null // Only correct if off by more than 10%

        // 6. Do not create a segment if targetCalories is null or not reasonable
        val targetCals = projection.targetCalories
        if (targetCals == null || targetCals !in 1200..6000) return null

        // 7. Build segment
        return GoalSegment(
            goalId = goalId,
            startDate = windowFirst.date,
            endDate = windowLast.date,
            startWeight = windowFirst.weight ?: return null,
            endWeight = windowLast.weight ?: return null,
            targetCalories = targetCals,
            ratePerWeek = actualRatePerWeek,
        )
    }

    // Helper for averageOrNull (null safe)
    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    // Extension for LocalDate to epoch millis
    private fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
