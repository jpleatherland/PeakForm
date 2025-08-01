package dev.jpleatherland.peakform.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startWeight: Double,
    val goalWeight: Double?, // nullable if duration based
    val type: GoalType, // TARGET_WEIGHT, CUSTOM
    val timeMode: GoalTimeMode, // BY_DATE, BY_RATE, BY_DURATION
    val targetDate: Long?, // if BY_DATE
    val ratePerWeek: Double?, // if BY_RATE
    val durationWeeks: Int?, // if BY_DURATION
    val rateMode: RateMode?, // if BY_RATE
    val ratePercent: Double? = null, // if rateMode is BODYWEIGHT_PERCENT
    val ratePreset: RatePreset? = null, // if rateMode is PRESET
    val createdAt: Long = System.currentTimeMillis(),
    val initialMaintenanceCalories: Int = 2000,
)

enum class GoalType {
    TARGET_WEIGHT, // user wants to reach a specific weight
    BULK, // user wants to gain weight. default +0.5% body weight / week
    CUT, // user wants to lose weight. default -0.5% body weight / week
    MAINTAIN, // user wants to maintain current weight
}

enum class GoalTimeMode {
    BY_DATE, // user specifies a target date
    BY_RATE, // user specifies a rate of weight change per week
    BY_DURATION, // user specifies a duration in weeks to reach the goal
}

enum class RateMode {
    KG_PER_WEEK,
    BODYWEIGHT_PERCENT,
    PRESET,
}

enum class RatePreset(
    val label: String,
    val percentPerWeek: Double,
) {
    LEAN("Lean", 0.003),
    MODERATE("Moderate", 0.005),
    AGGRESSIVE("Aggressive", 0.01),
}

data class GoalProgress(
    val targetCalories: Int,
    val estimatedGoalDate: Date?, // nullable if unknown
    val targetDate: Date?, // nullable if not set
    val isAheadOfSchedule: Boolean,
)
