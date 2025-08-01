import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalTimeMode
import dev.jpleatherland.peakform.data.GoalType
import dev.jpleatherland.peakform.util.GoalCalculations
import org.junit.Test

@Test
fun project_shouldCalculateCorrectTargetCalories() {
    val goal = Goal(
        type = GoalType.CUT,
        startWeight = 70.0,
        goalWeight = 66.0,
        timeMode = GoalTimeMode.BY_DATE,
        targetDate = fixedDatePlusWeeks(8),
        ...
    )
    val projection = GoalCalculations.project(goal, currentWeight = 70.0, avgMaintenance = 2400, ...)

    assertEquals(-0.5, projection.rateKgPerWeek, 0.01)
    assertEquals(1950, projection.targetCalories)
}
