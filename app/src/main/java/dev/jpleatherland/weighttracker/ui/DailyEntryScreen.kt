package dev.jpleatherland.weighttracker.ui

import android.app.DatePickerDialog
import android.icu.text.NumberFormat
import android.icu.util.Calendar
import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyEntryScreen(viewModel: WeightViewModel) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    // --- Date Picker State ---
    val calendar = remember { Calendar.getInstance() }

    var selectedDate by remember { mutableStateOf(calendar.time) }
    val formattedDate =
        remember(selectedDate) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate)
        }

    // --- Find Existing Entry for Date (ignores time, only date) ---
    fun Date.sameDayAs(other: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = this@sameDayAs }
        val cal2 = Calendar.getInstance().apply { time = other }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    val existingEntry =
        entries.find {
            it.date?.let { entryDate ->
                Date(entryDate).sameDayAs(selectedDate)
            } ?: false
        }

    // --- If user picks a date with an entry, prefill the fields ---
    LaunchedEffect(existingEntry) {
        if (existingEntry != null) {
            weight = String.format(Locale.getDefault(), "%.2f", existingEntry.weight ?: "")
            calories = existingEntry.calories?.toString() ?: ""
        } else {
            // Optionally clear fields if you want
            // weightInput = ""
            // caloriesInput = ""
        }
    }

    val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()
    val maintenance by viewModel.estimatedMaintenanceCalories.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val goalTimeMode = goal?.timeMode

    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

    Log.d("goalProgress", "Goal Progress: $goalProgress")

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
            Text("Date: $formattedDate")
        }

        // --- Existing Entry Warning ---
        if (existingEntry != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Warning: An entry for this date already exists. Saving will overwrite it.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Current entry: Weight = ${String.format(
                            Locale.getDefault(),
                            "%.2f",
                            existingEntry.weight ?: "-",
                        )} kg, Calories = ${existingEntry.calories ?: "-"} kcal",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

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

        Button(
            onClick = {
                val w = weight.toDoubleOrNull()
                val c = calories.toIntOrNull()

                if (w != null || c != null) {
                    viewModel.addEntry(w, c)
                    weight = ""
                    calories = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Entry")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        val numberFormat = NumberFormat.getNumberInstance()

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
                        Text(
                            "7-Day Rolling Average Weight",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = numberFormat.format(it),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Estimated Maintenance Calories",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "$maintenance kcal",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }

            goalProgress?.targetCalories?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Target Intake for Goal",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "$it kcal/day",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }

            goalProgress?.targetDate?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row {
                            Text("Target Date: ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = sdf.format(goalProgress?.targetDate!!),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (goalTimeMode == GoalTimeMode.BY_DATE) {
                goalProgress?.estimatedGoalDate?.let { estimatedDate ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text(
                                    "Estimated Date: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = sdf.format(estimatedDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            goalProgress?.isAheadOfSchedule?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "You're ahead of schedule!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
