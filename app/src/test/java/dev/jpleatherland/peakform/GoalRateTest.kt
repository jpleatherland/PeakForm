package dev.jpleatherland.peakform.data

import dev.jpleatherland.peakform.util.GoalCalculations
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import kotlin.math.abs

class GoalCalculationsTest {
    private val MAINTENANCE = 2500

    @Test
    fun `returns all nulls when no goal`() {
        val proj =
            GoalCalculations.project(
                goal = null,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "",
            )
        assertEquals(0.0, proj.rateKgPerWeek, 1e-6)
        assertNull(proj.goalDate)
        assertNull(proj.finalWeight)
        assertNull(proj.weightChange)
        assertNull(proj.totalCalories)
        assertNull(proj.dailyCalories)
        assertNull(proj.targetCalories)
    }

    @Test
    fun `cut by kg per week gives negative rate and calorie deficit`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = 65.0,
                type = GoalType.CUT,
                timeMode = GoalTimeMode.BY_RATE,
                ratePerWeek = 0.5,
                rateMode = RateMode.KG_PER_WEEK,
                ratePercent = null,
                ratePreset = null,
                durationWeeks = null,
                targetDate = null,
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "0.5",
            )
        assertTrue(proj.rateKgPerWeek < 0)
        assertTrue(proj.dailyCalories!! < 0)
        assertTrue(proj.targetCalories!! < MAINTENANCE)
        assertEquals(0.5 * -1, proj.rateKgPerWeek, 1e-6)
    }

    @Test
    fun `bulk by percent per week gives positive rate and surplus`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = 75.0,
                type = GoalType.BULK,
                timeMode = GoalTimeMode.BY_RATE,
                ratePerWeek = null,
                rateMode = RateMode.BODYWEIGHT_PERCENT,
                ratePercent = 1.0,
                ratePreset = null,
                durationWeeks = null,
                targetDate = null,
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "1.0",
            )
        assertTrue(proj.rateKgPerWeek > 0)
        assertTrue(proj.dailyCalories!! > 0)
        assertTrue(proj.targetCalories!! > MAINTENANCE)
        assertEquals(0.01 * 70.0, abs(proj.rateKgPerWeek), 1e-6)
    }

    @Test
    fun `target weight by preset gives correct rate and dates`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = 68.0,
                type = GoalType.TARGET_WEIGHT,
                timeMode = GoalTimeMode.BY_RATE,
                ratePerWeek = null,
                rateMode = RateMode.PRESET,
                ratePercent = null,
                ratePreset = RatePreset.MODERATE,
                durationWeeks = null,
                targetDate = null,
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "",
                selectedPreset = RatePreset.MODERATE,
            )
        assertEquals(-1.0 * RatePreset.MODERATE.percentPerWeek * 70.0, proj.rateKgPerWeek, 1e-6)
        assertNotNull(proj.goalDate)
    }

    @Test
    fun `by duration computes final weight and correct calories`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = null, // not needed for BY_DURATION
                type = GoalType.BULK,
                timeMode = GoalTimeMode.BY_DURATION,
                ratePerWeek = 0.2,
                rateMode = RateMode.KG_PER_WEEK,
                ratePercent = null,
                ratePreset = null,
                durationWeeks = 10,
                targetDate = null,
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "0.2",
                durationWeeks = 10,
            )
        assertEquals(72.0, proj.finalWeight!!, 1e-6)
        assertEquals(0.2, proj.rateKgPerWeek, 1e-6)
        assertTrue(proj.targetCalories!! > MAINTENANCE)
    }

    @Test
    fun `maintain goal always returns zeroes and nulls`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = null,
                type = GoalType.MAINTAIN,
                timeMode = GoalTimeMode.BY_RATE,
                ratePerWeek = 0.0,
                rateMode = RateMode.KG_PER_WEEK,
                ratePercent = null,
                ratePreset = null,
                durationWeeks = null,
                targetDate = null,
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "0",
            )
        assertEquals(0.0, proj.rateKgPerWeek, 1e-6)
        assertEquals(MAINTENANCE, proj.targetCalories)
        assertNull(proj.goalDate)
    }

    @Test
    fun `cut by date calculates positive weeks, negative rate`() {
        val goal =
            Goal(
                id = 0,
                goalWeight = 65.0,
                type = GoalType.CUT,
                timeMode = GoalTimeMode.BY_DATE,
                ratePerWeek = null,
                rateMode = RateMode.KG_PER_WEEK,
                ratePercent = null,
                ratePreset = null,
                durationWeeks = null,
                targetDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 1 week ahead
                createdAt = System.currentTimeMillis(),
            )
        val proj =
            GoalCalculations.project(
                goal = goal,
                currentWeight = 70.0,
                avgMaintenance = MAINTENANCE,
                rateInput = "",
                targetDate = Date(goal.targetDate!!),
            )
        assertTrue(proj.dailyCalories!! < 0)
        assertTrue(
            "Dates not within 1 second",
            kotlin.math.abs(goal.targetDate!! - proj.goalDate!!.time) <= 1000L,
        )
    }
}
