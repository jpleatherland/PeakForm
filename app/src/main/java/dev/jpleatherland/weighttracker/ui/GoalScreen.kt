package dev.jpleatherland.weighttracker.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalTimeMode
import dev.jpleatherland.weighttracker.data.GoalType
import dev.jpleatherland.weighttracker.data.RateMode
import dev.jpleatherland.weighttracker.data.RatePreset
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun GoalScreen(viewModel: WeightViewModel) {
    val context = LocalContext.current
    val currentGoal by viewModel.goal.collectAsState()

    var goalType by remember { mutableStateOf(GoalType.CUT) }
    var goalWeight by remember { mutableStateOf("") }

    var timeMode by remember { mutableStateOf(GoalTimeMode.BY_DURATION) }
    var targetDate by remember { mutableStateOf<Date?>(null) }
    var ratePerWeek by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("") }

    val needsGoalWeight =
        when (goalType) {
            GoalType.TARGET_WEIGHT -> true
            GoalType.CUT, GoalType.BULK -> timeMode != GoalTimeMode.BY_DURATION
            GoalType.MAINTAIN -> false
        }

    val showDatePicker = timeMode == GoalTimeMode.BY_DATE
    val showRateField = timeMode == GoalTimeMode.BY_RATE
    val showDurationField = timeMode == GoalTimeMode.BY_DURATION

    var rateMode by remember { mutableStateOf(RateMode.PRESET) }
    var rateInput by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(RatePreset.LEAN) }

    val currentWeight =
        viewModel.entries.value
            .lastOrNull()
            ?.weight ?: 70.0 // fallback

    val calculatedRateKgPerWeek: Double =
        when (rateMode) {
            RateMode.KG_PER_WEEK -> rateInput.toDoubleOrNull() ?: 0.0
            RateMode.BODYWEIGHT_PERCENT -> {
                val percent = rateInput.toDoubleOrNull() ?: 0.0
                (percent / 100.0) * currentWeight
            }
            RateMode.PRESET -> selectedPreset.percentPerWeek * currentWeight
        }

    val estimatedGoalDate: Date? =
        if (
            timeMode == GoalTimeMode.BY_RATE &&
            needsGoalWeight &&
            goalWeight.toDoubleOrNull() != null &&
            calculatedRateKgPerWeek > 0.0
        ) {
            val current = currentWeight
            val target = goalWeight.toDouble()
            val delta = abs(target - current)
            val weeks = delta / calculatedRateKgPerWeek
            val millisUntilGoal = (weeks * 7 * 24 * 60 * 60 * 1000).toLong()
            Date(System.currentTimeMillis() + millisUntilGoal)
        } else {
            null
        }

    val estimatedRateKgPerWeek: Double? =
        if (
            timeMode == GoalTimeMode.BY_DATE &&
            needsGoalWeight &&
            goalWeight.toDoubleOrNull() != null &&
            targetDate != null
        ) {
            val current = currentWeight
            val target = goalWeight.toDouble()
            val delta = abs(target - current)

            val now = System.currentTimeMillis()
            val millisDiff = targetDate!!.time - now
            val weeks = millisDiff / (1000.0 * 60 * 60 * 24 * 7)

            if (weeks > 0) delta / weeks else null
        } else {
            null
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Set Your Goal", style = MaterialTheme.typography.headlineSmall)

        // Goal Type Dropdown
        DropdownSelector(
            label = "Goal Type",
            options = GoalType.entries,
            selected = goalType,
            onSelected = { goalType = it },
        )

        if (needsGoalWeight) {
            OutlinedTextField(
                value = goalWeight,
                onValueChange = { goalWeight = it },
                label = { Text("Target Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        // Time Mode Dropdown
        DropdownSelector(
            label = "Time Mode",
            options = GoalTimeMode.entries,
            selected = timeMode,
            onSelected = { timeMode = it },
        )

        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            OutlinedButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        targetDate = calendar.time
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                ).show()
            }) {
                Text(
                    "Select Target Date: ${
                        targetDate?.let {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                        } ?: "None"
                    }",
                )
            }
        }

        estimatedRateKgPerWeek?.let { rate ->
            Text(
                text = String.format(Locale.UK, "Required Rate: %.2f kg/week", rate),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (showRateField) {
            DropdownSelector(
                label = "Rate Mode",
                options = RateMode.entries,
                selected = rateMode,
                onSelected = { rateMode = it },
            )

            // Then the corresponding input for the selected mode
            when (rateMode) {
                RateMode.KG_PER_WEEK -> {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Rate (kg/week)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                RateMode.BODYWEIGHT_PERCENT -> {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Rate (% bodyweight/week)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                RateMode.PRESET -> {
                    DropdownSelector(
                        label = "Preset",
                        options = RatePreset.entries,
                        selected = selectedPreset,
                        onSelected = { selectedPreset = it },
                    )
                }
            }
        }

        if (showDurationField) {
            OutlinedTextField(
                value = durationWeeks,
                onValueChange = { durationWeeks = it },
                label = { Text("Duration (weeks)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        Button(onClick = {
            val goal =
                Goal(
                    goalWeight = goalWeight.toDoubleOrNull(),
                    type = goalType,
                    timeMode = timeMode,
                    targetDate = targetDate?.time,
                    ratePerWeek = ratePerWeek.toDoubleOrNull(),
                    durationWeeks = durationWeeks.toIntOrNull(),
                )
            viewModel.setGoal(goal)
        }) {
            Text("Save Goal")
        }

        currentGoal?.let {
            Text("Current goal set: ${it.type} (${it.timeMode})", style = MaterialTheme.typography.bodySmall)
        }
        estimatedGoalDate?.let { date ->
            Text(
                text = "Estimated Goal Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun <T : Enum<T>> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label: ${selected.name.formatEnumName()}")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.name
                                .lowercase()
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() },
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

fun String.formatEnumName(): String =
    this
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
