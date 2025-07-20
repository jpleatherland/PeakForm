package dev.jpleatherland.weighttracker.util

import android.util.Log
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.data.GoalType
import dev.jpleatherland.weighttracker.data.RateMode
import java.util.*
import kotlin.math.abs

object GoalCalculations {
    fun estimateFinalWeight(
        currentWeight: Double,
        goalType: GoalType,
        timeMode: GoalTimeMode,
        durationWeeks: Int?,
        rateKgPerWeek: Double,
    ): Double? {
        if (timeMode != GoalTimeMode.BY_DURATION || durationWeeks == null) return null
        val delta = rateKgPerWeek * durationWeeks
        return when (goalType) {
            GoalType.CUT -> currentWeight - delta
            GoalType.BULK -> currentWeight + delta
            else -> null
        }
    }

    fun estimateGoalDate(
        currentWeight: Double,
        goalWeight: Double?,
        rateKgPerWeek: Double,
        timeMode: GoalTimeMode,
        goalType: GoalType,
    ): Date? {
        if (timeMode != GoalTimeMode.BY_RATE || goalWeight == null || rateKgPerWeek <= 0.0) return null
        val delta = abs(goalWeight - currentWeight)
        val weeks = delta / rateKgPerWeek
        val millisUntilGoal = (weeks * 7 * 24 * 60 * 60 * 1000).toLong()
        return Date(System.currentTimeMillis() + millisUntilGoal)
    }

    fun estimateRatePerWeek(
        currentWeight: Double,
        goalWeight: Double?,
        targetDate: Date?,
        timeMode: GoalTimeMode,
        goalType: GoalType,
    ): Double? {
        if (timeMode != GoalTimeMode.BY_DATE || goalWeight == null || targetDate == null) return null
        val delta = abs(goalWeight - currentWeight)
        val now = System.currentTimeMillis()
        val millisDiff = targetDate.time - now
        val weeks = millisDiff / (1000.0 * 60 * 60 * 24 * 7)
        return if (weeks > 0) delta / weeks else null
    }

    fun estimateCalories(
        currentWeight: Double,
        goalWeight: Double?,
        durationWeeks: Int?,
        estimatedGoalDate: Date?,
        targetDate: Date?,
        rateKgPerWeek: Double,
        timeMode: GoalTimeMode,
        goalType: GoalType,
    ): Pair<Int?, Int?> {
        if (goalType == GoalType.MAINTAIN) return 0 to 0
        if (goalType != GoalType.CUT && goalType != GoalType.BULK && goalType != GoalType.TARGET_WEIGHT) return null to null

        val weightChange =
            when {
                timeMode == GoalTimeMode.BY_DURATION && durationWeeks != null ->
                    rateKgPerWeek * durationWeeks

                timeMode == GoalTimeMode.BY_RATE && goalWeight != null ->
                    goalWeight - currentWeight

                timeMode == GoalTimeMode.BY_DATE && goalWeight != null ->
                    goalWeight - currentWeight

                else -> return null to null
            }

        val signedWeightChange =
            when (goalType) {
                GoalType.CUT -> -abs(weightChange)
                GoalType.BULK -> abs(weightChange)
                GoalType.TARGET_WEIGHT -> weightChange
                else -> 0.0
            }

        val totalCalories = (signedWeightChange * 7700).toInt()

        val days =
            when {
                timeMode == GoalTimeMode.BY_DURATION && durationWeeks != null && durationWeeks > 0 -> durationWeeks * 7
                timeMode == GoalTimeMode.BY_DATE && targetDate != null ->
                    ((targetDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                else -> null
            }

        val dailyCalories =
            when (timeMode) {
                GoalTimeMode.BY_RATE -> (rateKgPerWeek * 7700 / 7).toInt()
                else ->
                    days?.takeIf { it > 0 }?.let { totalCalories / it }
            }
        Log.d(
            "util",
            ": timeMode=$timeMode durationWeeks=$durationWeeks rateKgPerWeek=$rateKgPerWeek days=$days totalCalories=$totalCalories dailyCalories=$dailyCalories",
        )
        return totalCalories to dailyCalories
    }

    fun reconstructRateKgPerWeek(
        goal: Goal,
        currentWeight: Double,
    ): Double {
        // 1. If user set a direct value, use it.
        goal.ratePerWeek?.let { return it }

        // 2. If user chose percent, calculate from percent & weight.
        if (goal.rateMode == RateMode.BODYWEIGHT_PERCENT && goal.ratePercent != null) {
            return (goal.ratePercent / 100.0) * currentWeight
        }

        // 3. If user chose preset, use that.
        if (goal.rateMode == RateMode.PRESET && goal.ratePreset != null) {
            return goal.ratePreset.percentPerWeek * currentWeight
        }

        // 4. If none, fallback to a sensible default (e.g. 0.5%/wk for cut/bulk).
        return when (goal.type) {
            GoalType.CUT, GoalType.BULK ->
                0.005 * currentWeight // 0.5% of bodyweight per week
            else -> 0.0
        }
    }
}
