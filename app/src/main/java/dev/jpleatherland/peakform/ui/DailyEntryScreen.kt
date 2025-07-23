package dev.jpleatherland.peakform.ui

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.peakform.data.GoalType
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.util.GoalCalculations
import dev.jpleatherland.peakform.util.asDayEpochMillis
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyEntryScreen(viewModel: WeightViewModel) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    val today = remember { calendar.time }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    // --- Find Existing Entry for Date (ignores time, only date) ---
    fun Date.sameDayAs(other: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = this@sameDayAs }
        val cal2 = Calendar.getInstance().apply { time = other }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    LaunchedEffect(entries, selectedDate) {
        if (selectedDate == null && entries.isNotEmpty()) {
            val alreadyLoggedToday =
                entries.any { entry ->
                    entry.date.let {
                        Date(it).sameDayAs(today)
                    }
                }
            selectedDate =
                if (alreadyLoggedToday) null else today
        }
    }

    val existingEntry =
        selectedDate?.let { nonNullDate ->
            entries.find {
                it.date.let { entryDate -> Date(entryDate).sameDayAs(nonNullDate) }
            }
        }

    LaunchedEffect(existingEntry) {
        if (existingEntry != null) {
            weight = String.format(Locale.getDefault(), "%.2f", existingEntry.weight ?: "")
            calories = existingEntry.calories?.toString() ?: ""
        } else {
            weight = ""
            calories = ""
        }
    }

    val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()
    val maintenance by viewModel.estimatedMaintenanceCalories.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState() // If you want to keep using this, fine!

    // Use 7-day avg if present, else last entry, else fallback
    val currentWeight = avgWeight ?: entries.lastOrNull()?.weight ?: 70.0

    // Set up projection using most recent Goal + current weight and maintenance
    val projection =
        GoalCalculations.project(
            goal = goal,
            currentWeight = currentWeight,
            avgMaintenance = maintenance,
            rateInput =
                when (goal?.rateMode) {
                    RateMode.KG_PER_WEEK -> goal?.ratePerWeek?.toString() ?: ""
                    RateMode.BODYWEIGHT_PERCENT -> goal?.ratePercent?.toString() ?: ""
                    RateMode.PRESET -> "" // not used
                    else -> ""
                },
            selectedPreset = goal?.ratePreset,
            durationWeeks = goal?.durationWeeks,
            targetDate = goal?.targetDate?.let { Date(it) },
        )

    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
    val numberFormat = NumberFormat.getNumberInstance()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- Date Picker ---
        OutlinedButton(
            onClick = {
                val now = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        selectedDate = calendar.time
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        ) {
            selectedDate?.let { nonNullDate ->
                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(nonNullDate)
                Text(formattedDate)
            }
                ?: Text("Select Date")
        }
        selectedDate?.let { nonNullDate ->
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Enter weight (kg)") },
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Enter calories") },
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Existing entry warning
            existingEntry?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "An entry for this date already exists. Saving will overwrite it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val w = weight.toDoubleOrNull()
                    val c = calories.toIntOrNull()
                    val d = nonNullDate.asDayEpochMillis()

                    if (w != null || c != null) {
                        viewModel.addEntry(w, c, d) { success ->
                            if (success) {
                                Toast.makeText(context, "Entry saved successfully", Toast.LENGTH_SHORT).show()
                                weight = ""
                                calories = ""
                                selectedDate = null
                            } else {
                                Log.e("DailyEntryScreen", "Failed to save entry")
                                Toast.makeText(context, "Failed to save entry", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Entry")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Metrics", style = MaterialTheme.typography.headlineSmall)

            avgWeight?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("7-Day Rolling Average Weight", style = MaterialTheme.typography.labelLarge)
                        Text(text = numberFormat.format(it), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            maintenance?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Estimated Maintenance Calories", style = MaterialTheme.typography.labelLarge)
                        Text(text = "$it kcal", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            projection.targetCalories?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Target Intake for Goal", style = MaterialTheme.typography.labelLarge)
                        Text(text = "$it kcal/day", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            projection.goalDate?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row {
                            Text("Goal Date: ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = sdf.format(it),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            projection.finalWeight?.let { weight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = String.format(Locale.UK, "Estimated Final Weight: %.1f kg", weight),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            projection.weightChange?.let { change ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = String.format(Locale.UK, "Estimated Weight Change: %.1f kg", change),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Optional: Show required daily change for bulk/cut/target
            if (goal?.type == GoalType.CUT || goal?.type == GoalType.BULK || goal?.type == GoalType.TARGET_WEIGHT) {
                projection.dailyCalories?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Required Daily Calorie Change: $it kcal/day",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
