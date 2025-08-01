package dev.jpleatherland.peakform.ui

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.peakform.data.GoalTimeMode
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.util.GoalCalculations
import dev.jpleatherland.peakform.util.asDayEpochMillis
import dev.jpleatherland.peakform.util.formatWeight
import dev.jpleatherland.peakform.util.kgToLb
import dev.jpleatherland.peakform.util.lbToKg
import dev.jpleatherland.peakform.viewmodel.SettingsViewModel
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyEntryScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val weightUnit by settingsViewModel.weightUnit.collectAsState()
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    val today = remember { calendar.time }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    fun Date.sameDayAs(other: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = this@sameDayAs }
        val cal2 = Calendar.getInstance().apply { time = other }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    LaunchedEffect(entries, selectedDate) {
        if (selectedDate == null && entries.isNotEmpty()) {
            val alreadyLoggedToday =
                entries.any { entry -> Date(entry.date).sameDayAs(today) }
            selectedDate =
                if (alreadyLoggedToday) null else today
        }
    }

    val existingEntry =
        selectedDate?.let { nonNullDate ->
            entries.find { Date(it.date).sameDayAs(nonNullDate) }
        }

    // Show previous entry in user's preferred unit
    LaunchedEffect(existingEntry, weightUnit) {
        if (existingEntry != null) {
            val entryWeightKg = existingEntry.weight ?: 0.0
            val entryWeightDisplay =
                when (weightUnit) {
                    WeightUnit.KG -> entryWeightKg
                    WeightUnit.LB -> entryWeightKg.kgToLb()
                }
            weight = String.format(Locale.getDefault(), "%.2f", entryWeightDisplay)
            calories = existingEntry.calories?.toString() ?: ""
        } else {
            weight = ""
            calories = ""
        }
    }

    val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()
    val maintenance by viewModel.estimatedMaintenanceCalories.collectAsState()
    val maintenanceEntryCount by viewModel.maintenanceEntryCount.collectAsState()
    val maintenanceEstimateErrorMessage by viewModel.maintenanceEstimateErrorMessage.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()

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

    // --- Weight Input Parsing ---
    fun parseWeightInput(text: String): Double? =
        text.toDoubleOrNull()?.let { value ->
            when (weightUnit) {
                WeightUnit.KG -> value
                WeightUnit.LB -> value.lbToKg()
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp, 8.dp, 16.dp, 0.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                label = { Text("Enter weight (${weightUnit.name.lowercase()})") },
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
                    val w = parseWeightInput(weight)
                    val c = calories.toIntOrNull()
                    val d = nonNullDate.asDayEpochMillis()
                    val ws = if (w != null) "user" else null
                    val cs = if (c != null) "user" else null

                    if (w != null || c != null) {
                        viewModel.addEntry(w, c, d, ws, cs) { success ->
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

        HorizontalDivider()

        Text("Metrics", style = MaterialTheme.typography.headlineSmall)

        avgWeight?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("7-Day Rolling Average Weight", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatWeight(it, weightUnit),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (maintenance != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Estimated Maintenance Calories", style = MaterialTheme.typography.titleMedium)
                    Text(text = "$maintenance kcal", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Based on $maintenanceEntryCount entries in the last 14 days",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else if (maintenanceEstimateErrorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Estimated Maintenance Calories", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$maintenanceEstimateErrorMessage",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Based on $maintenanceEntryCount entries in the last 14 days",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Goal Details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                goal?.let {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Current Goal:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text =
                                it.type.name
                                    .lowercase()
                                    .replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    projection.targetCalories?.let { targetCalories ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Target Intake:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                "$targetCalories kcal/day",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                    projection.dailyCalories?.let { dailyCalories ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Daily Calorie Change:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                "$dailyCalories kcal/day",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                    projection.goalDate?.let { goalDate ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val label =
                                when (goal?.timeMode) {
                                    GoalTimeMode.BY_DURATION, GoalTimeMode.BY_RATE -> "Goal Date:"
                                    GoalTimeMode.BY_DATE -> "Target Goal Date:"
                                    else -> "Goal Date:"
                                }
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                sdf.format(goalDate),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                    projection.finalWeight?.let { finalWeight ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val label =
                                when (goal?.timeMode) {
                                    GoalTimeMode.BY_DURATION -> "Estimated Final Weight:"
                                    GoalTimeMode.BY_DATE, GoalTimeMode.BY_RATE -> "Target Weight:"
                                    else -> "Final Weight:"
                                }
                            val displayWeight = formatWeight(finalWeight, weightUnit)
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                displayWeight,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                    projection.weightChange?.let { weightChange ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Weight Remaining:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                formatWeight(weightChange, weightUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                } ?: Text("No active goal set", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
