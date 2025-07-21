import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.data.GoalType
import dev.jpleatherland.weighttracker.data.RateMode
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.util.GoalCalculations.generateSegment
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.util.*

class GoalSegmentTest {
    @Test
    fun `generateSegment only creates correction segment when deviation exceeds 10 percent`() {
        // --- Goal: Lose 0.5kg/week, starting at 70kg ---
        val goal =
            Goal(
                id = 1,
                type = GoalType.CUT,
                timeMode = GoalTimeMode.BY_RATE,
                rateMode = RateMode.KG_PER_WEEK,
                ratePerWeek = 0.5,
                goalWeight = 65.0,
                targetDate = null,
                durationWeeks = null,
            )
        val maintenance = 2200

        val startDate = LocalDate.now().minusDays(13)
        val startWeight = 70.0
        val endWeight = 69.0 // 1.0kg drop in 14 days = 0.5kg/week

        // 14 perfectly spaced entries, linearly from 70.0 → 69.0
        val normalEntries =
            (0 until 14).map { i ->
                WeightEntry(
                    id = i,
                    date = startDate.plusDays(i.toLong()).toEpochMillis(),
                    weight = startWeight - ((startWeight - endWeight) * i / 13.0),
                    calories = 1900,
                )
            }

        // On track: should return null (no correction needed)
        val segmentOnTrack =
            generateSegment(
                goal = goal,
                entries = normalEntries,
                estimatedMaintenance = maintenance,
                rateInput = "0.5",
            )
        assertNull("No correction segment should be created when on track", segmentOnTrack)

        // Now, simulate a 2.0kg drop in just 5 days (faster than plan)
        val newStartWeight = normalEntries.last().weight ?: 0.0
        val fastLossEntries =
            (0 until 5).map { i ->
                WeightEntry(
                    id = 100 + i,
                    date = startDate.plusDays(14 + i.toLong()).toEpochMillis(),
                    weight = newStartWeight - (2.0 * i / 4.0), // 2kg drop in 5 days
                    calories = 1300,
                )
            }
        val allEntries = normalEntries + fastLossEntries

        val segmentOffTrack =
            generateSegment(
                goal = goal,
                entries = allEntries,
                estimatedMaintenance = maintenance,
                rateInput = "0.5",
            )

        assertNotNull("A correction segment should be generated for >10% deviation", segmentOffTrack)
        with(segmentOffTrack!!) {
            assertTrue("Correction segment should reflect a cut (weight loss)", endWeight < startWeight)
        }
    }

    // Helper: LocalDate to millis
    private fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
