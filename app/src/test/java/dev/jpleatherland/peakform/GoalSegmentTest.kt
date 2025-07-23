import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalSegment
import dev.jpleatherland.peakform.data.GoalSegmentDao
import dev.jpleatherland.peakform.data.GoalSegmentRepository
import dev.jpleatherland.peakform.data.GoalTimeMode
import dev.jpleatherland.peakform.data.GoalType
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.data.WeightEntry
import dev.jpleatherland.peakform.dev.TestDataType
import dev.jpleatherland.peakform.util.GoalCalculations.maybeGenerateCorrectionSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import kotlin.random.Random

class FakeGoalSegmentDao(
    private val segments: List<GoalSegment> = emptyList(),
) : GoalSegmentDao {
    override fun getAllSegmentsForGoal(goalId: Int): Flow<List<GoalSegment>> {
        // Return only segments matching the requested goalId
        return flowOf(segments.filter { it.goalId == goalId })
    }

    override suspend fun insert(segment: GoalSegment): Long {
        // Optionally, add logic to add to a mutable list if you want to test inserts.
        return 1L // Or a fake row ID
    }

    override suspend fun clearSegmentsForGoal(goalId: Int) {
        // No-op for test
    }

    override suspend fun clearAllGoalSegments() {
        // No-op for test
    }
}

class GoalSegmentTest {
    private fun defaultGoal(
        type: TestDataType,
        createdAt: Long,
    ): Goal =
        when (type) {
            TestDataType.TRENDING_DOWN, TestDataType.CUT_WITH_JUMP ->
                Goal(
                    id = 1,
                    goalWeight = 62.0,
                    type = GoalType.CUT,
                    timeMode = GoalTimeMode.BY_RATE,
                    targetDate = null,
                    ratePerWeek = 0.5,
                    durationWeeks = null,
                    rateMode = RateMode.KG_PER_WEEK,
                    createdAt = createdAt,
                )
            TestDataType.TRENDING_UP, TestDataType.BULK_WITH_JUMP, TestDataType.BULK_WITH_JUMP_BY_DATE ->
                Goal(
                    id = 1,
                    goalWeight = 78.0,
                    type = GoalType.BULK,
                    timeMode = GoalTimeMode.BY_RATE,
                    targetDate = null,
                    ratePerWeek = 0.3,
                    durationWeeks = null,
                    rateMode = RateMode.KG_PER_WEEK,
                    createdAt = createdAt,
                )
            TestDataType.RANDOM ->
                Goal(
                    id = 1,
                    goalWeight = 70.0,
                    type = GoalType.TARGET_WEIGHT,
                    timeMode = GoalTimeMode.BY_DATE,
                    targetDate = System.currentTimeMillis() + 14 * 24 * 3600 * 1000,
                    ratePerWeek = null,
                    durationWeeks = null,
                    rateMode = null,
                    createdAt = createdAt,
                )
        }

    @Test
    fun `simulate user entering each day and check for segment creation timing`() =
        runBlocking {
            val bulkRatePerDay = 0.3 / 7
            val today = LocalDate.now()
            val startDate = today.minusDays(29)
            val goal =
                Goal(
                    id = 1,
                    goalWeight = 78.0,
                    type = GoalType.BULK,
                    timeMode = GoalTimeMode.BY_RATE,
                    rateMode = RateMode.KG_PER_WEEK,
                    ratePerWeek = 0.3,
                    durationWeeks = null,
                    createdAt = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    targetDate = null,
                )
            val fakeDao = FakeGoalSegmentDao()
            val repo = GoalSegmentRepository(fakeDao)

            val entries = mutableListOf<WeightEntry>()
            var correctionCreatedDay: Int? = null

            for (i in 0 until 30) {
                val date = startDate.plusDays(i.toLong())
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val weight = 68.0 + (i * bulkRatePerDay)
                val calories = 2500

                entries.add(WeightEntry(date = dateMillis, weight = weight, calories = calories))

                val segment =
                    maybeGenerateCorrectionSegment(
                        goal = goal,
                        entries = entries.toList(),
                        estimatedMaintenance = 2700,
                        goalSegmentRepository = repo,
                    )

                if (segment != null && correctionCreatedDay == null) {
                    correctionCreatedDay = i
                }
            }

            // For on-track bulk, expect NO correction segment at any point
            assertNull("No correction segment should be created for steady on-track entries", correctionCreatedDay)
        }

    @Test
    fun `simulate daily entry with bulk with jump pattern triggers correction at right time`() =
        runBlocking {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)
            val goal =
                Goal(
                    id = 1,
                    goalWeight = 78.0,
                    type = GoalType.BULK,
                    timeMode = GoalTimeMode.BY_RATE,
                    rateMode = RateMode.KG_PER_WEEK,
                    ratePerWeek = 0.3,
                    durationWeeks = null,
                    createdAt = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    targetDate = null,
                )
            val fakeDao = FakeGoalSegmentDao()
            val repo = GoalSegmentRepository(fakeDao)

            val entries = mutableListOf<WeightEntry>()
            var correctionCreatedDay: Int? = null
            var correctionSegment: GoalSegment? = null

            for (i in 0 until 30) {
                val date = startDate.plusDays(i.toLong())
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val weight =
                    if (i < 15) {
                        68.0 + (i * 0.1)
                    } else {
                        61.0 + ((i - 15) * 0.3)
                    }
                val calories = 2500

                entries.add(WeightEntry(date = dateMillis, weight = weight, calories = calories))

                val segment =
                    maybeGenerateCorrectionSegment(
                        goal = goal,
                        entries = entries.toList(),
                        estimatedMaintenance = 2700,
                        goalSegmentRepository = repo,
                    )

                if (segment != null && correctionCreatedDay == null) {
                    correctionCreatedDay = i
                    correctionSegment = segment
                }
            }

            // For this scenario, a correction segment should eventually be created
            assertNotNull("Correction segment should be created due to jump in bulk pattern", correctionCreatedDay)

            // Optionally, print out the day and the segment for debugging
            println("Correction segment created on day: $correctionCreatedDay, segment: $correctionSegment")
        }

    @Test
    fun `segment generated for BULK_WITH_JUMP but not for TRENDING_UP`() =
        runBlocking {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)

            val fakeDao = FakeGoalSegmentDao()
            val repo = GoalSegmentRepository(fakeDao)

            // TRENDING_UP: should NOT generate a segment (no jump)
            val trendingUpEntries = syntheticEntries(TestDataType.TRENDING_UP, startDate)
            val trendingUpGoal = defaultGoal(TestDataType.TRENDING_UP, trendingUpEntries.first().date)
            val segmentTrendingUp =
                maybeGenerateCorrectionSegment(
                    goal = trendingUpGoal,
                    entries = trendingUpEntries,
                    estimatedMaintenance = 2700,
                    goalSegmentRepository = repo,
                )
            assertNull("Should not create segment for steady bulk trend", segmentTrendingUp)

            // BULK_WITH_JUMP: should generate a segment (big deviation)
            val bulkWithJumpEntries = syntheticEntries(TestDataType.BULK_WITH_JUMP, startDate)
            val bulkWithJumpGoal = defaultGoal(TestDataType.BULK_WITH_JUMP, bulkWithJumpEntries.first().date)
            val segmentBulkWithJump =
                maybeGenerateCorrectionSegment(
                    goal = bulkWithJumpGoal,
                    entries = bulkWithJumpEntries,
                    estimatedMaintenance = 2700,
                    goalSegmentRepository = repo,
                )
            assertNotNull("Should create segment for bulk with jump scenario", segmentBulkWithJump)
        }

    @Test
    fun `generateSegment only creates correction segment when deviation exceeds 10 percent`() =
        runBlocking {
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

            val fakeDao = FakeGoalSegmentDao()
            val repo = GoalSegmentRepository(fakeDao)

            // On track: should return null (no correction needed)
            val segmentOnTrack =
                maybeGenerateCorrectionSegment(
                    goal = goal,
                    entries = normalEntries,
                    estimatedMaintenance = maintenance,
                    goalSegmentRepository = repo,
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
                maybeGenerateCorrectionSegment(
                    goal = goal,
                    entries = allEntries,
                    estimatedMaintenance = maintenance,
                    goalSegmentRepository = repo,
                )

            assertNotNull("A correction segment should be generated for >10% deviation", segmentOffTrack)
            with(segmentOffTrack!!) {
                assertTrue("Correction segment should reflect a cut (weight loss)", endWeight < startWeight)
            }
        }

    // Helper: LocalDate to millis
    private fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun syntheticEntries(
        type: TestDataType,
        startDate: LocalDate,
        days: Int = 30,
    ): List<WeightEntry> =
        (0 until days).map { i ->
            val date = startDate.plusDays(i.toLong())
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weight =
                when (type) {
                    TestDataType.TRENDING_UP -> 68.0 + (i * 0.042857)
                    TestDataType.TRENDING_DOWN -> 72.0 - (i * (0.5 / 7))
                    TestDataType.BULK_WITH_JUMP ->
                        if (i < 15) {
                            68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                        } else {
                            61.0 + ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                        }
                    TestDataType.CUT_WITH_JUMP ->
                        if (i < 15) {
                            72.0 - (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                        } else {
                            78.0 - ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                        }
                    TestDataType.RANDOM -> 70.0 + Random.nextDouble(-1.5, 1.5)
                    TestDataType.BULK_WITH_JUMP_BY_DATE ->
                        if (i < 15) {
                            68.0 + (i * 0.1) + Random.nextDouble(-0.2, 0.2)
                        } else {
                            61.0 + ((i - 15) * 0.3) + Random.nextDouble(-0.3, 0.3)
                        }
                }
            val calories = 2500 + Random.nextInt(-400, 400)
            WeightEntry(date = dateMillis, weight = weight, calories = calories)
        }
}
