import dev.jpleatherland.peakform.data.WeightEntry
import dev.jpleatherland.peakform.util.toEpochMillis
import org.junit.Test
import java.time.LocalDate

@Test
fun maintenanceEstimate_losingHalfKiloPerWeek_returnsCorrectMaintenance() {
    val baseDate = LocalDate.of(2025, 1, 1)
    val entries =
        (0..7).map { day ->
            WeightEntry(
                date = baseDate.plusDays(day.toLong()).toEpochMillis(),
                weight = 70.0 - (day * (0.5 / 7)), // lose 0.5kg over 7 days
                calories = 2200,
            )
        }

    val estimated = MaintenanceCalculator.estimate(entries)
    assertEquals(2585, estimated)
}
