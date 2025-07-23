package dev.jpleatherland.peakform.ui

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalTimeMode
import dev.jpleatherland.peakform.data.GoalType
import dev.jpleatherland.peakform.data.RateMode
import dev.jpleatherland.peakform.data.RatePreset
import dev.jpleatherland.peakform.util.GoalCalculations
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GoalScreen(viewModel: WeightViewModel) {
    val context = LocalContext.current
    val currentGoal by viewModel.goal.collectAsState()
    val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()
    val maintenance by viewModel.estimatedMaintenanceCalories.collectAsState()

    // UI state variables for goal editor
    var goalType by remember { mutableStateOf(GoalType.CUT) }
    var goalWeight by remember { mutableStateOf("") }
    var timeMode by remember { mutableStateOf(GoalTimeMode.BY_DURATION) }
    var targetDate by remember { mutableStateOf<Date?>(null) }
    var rateMode by remember { mutableStateOf(RateMode.PRESET) }
    var rateInput by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(RatePreset.LEAN) }
    var durationWeeks by remember { mutableStateOf("") }

    // On goal change, update form state
    LaunchedEffect(currentGoal) {
        currentGoal?.let { goal ->
            goalType = goal.type
            goalWeight = goal.goalWeight?.toString() ?: ""
            timeMode = goal.timeMode
            targetDate = goal.targetDate?.let { Date(it) }
            durationWeeks = goal.durationWeeks?.toString() ?: ""
            rateMode = goal.rateMode ?: RateMode.PRESET
            selectedPreset = goal.ratePreset ?: RatePreset.LEAN
            rateInput =
                when (rateMode) {
                    RateMode.KG_PER_WEEK -> goal.ratePerWeek?.toString() ?: ""
                    RateMode.BODYWEIGHT_PERCENT -> goal.ratePercent?.toString() ?: ""
                    RateMode.PRESET -> "" // No input for preset
                }
        }
    }

    val needsGoalWeight =
        when (goalType) {
            GoalType.TARGET_WEIGHT -> true
            GoalType.CUT, GoalType.BULK -> timeMode != GoalTimeMode.BY_DURATION
            GoalType.MAINTAIN -> false
        }

    val showDatePicker = timeMode == GoalTimeMode.BY_DATE
    val showRateField = timeMode in setOf(GoalTimeMode.BY_RATE, GoalTimeMode.BY_DURATION)
    val showDurationField = timeMode == GoalTimeMode.BY_DURATION

    // Figure out weight (fallback to last logged, then 70)
    val currentWeight =
        avgWeight
            ?: viewModel.entries.value
                .lastOrNull()
                ?.weight
            ?: 70.0

    // Centralized calculation/projection!
    val projection =
        GoalCalculations.project(
            goal =
                currentGoal?.copy(
                    // Override with live UI state so projections update before saving
                    type = goalType,
                    goalWeight = goalWeight.toDoubleOrNull(),
                    timeMode = timeMode,
                    targetDate = targetDate?.time,
                    rateMode = rateMode,
                    ratePercent = if (rateMode == RateMode.BODYWEIGHT_PERCENT) rateInput.toDoubleOrNull() else null,
                    ratePreset = if (rateMode == RateMode.PRESET) selectedPreset else null,
                    durationWeeks = durationWeeks.toIntOrNull(),
                ) ?: Goal(
                    type = goalType,
                    goalWeight = goalWeight.toDoubleOrNull(),
                    timeMode = timeMode,
                    targetDate = targetDate?.time,
                    rateMode = rateMode,
                    ratePercent = if (rateMode == RateMode.BODYWEIGHT_PERCENT) rateInput.toDoubleOrNull() else null,
                    ratePreset = if (rateMode == RateMode.PRESET) selectedPreset else null,
                    ratePerWeek = if (rateMode == RateMode.KG_PER_WEEK) rateInput.toDoubleOrNull() else null,
                    durationWeeks = durationWeeks.toIntOrNull(),
                ),
            currentWeight = currentWeight,
            avgMaintenance = maintenance,
            rateInput = rateInput,
            selectedPreset = selectedPreset,
            durationWeeks = durationWeeks.toIntOrNull(),
            targetDate = targetDate,
        )

    // Determine impossible goal
    val isImpossibleGoal: Boolean =
        projection.dailyCalories == null ||
            when (goalType) {
                GoalType.CUT -> projection.targetCalories != null && projection.targetCalories < 1000
                GoalType.BULK -> projection.dailyCalories > 2000
                GoalType.TARGET_WEIGHT ->
                    (projection.targetCalories != null && projection.targetCalories < 1000) ||
                        (projection.dailyCalories > 2000)
                else -> false
            }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        avgWeight?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("7-Day Rolling Average Weight", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = String.format(Locale.UK, "%.2f kg", it),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text("Estimated Maintenance", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "$maintenance kcal/day",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text("Set Your Goal", style = MaterialTheme.typography.headlineSmall)

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

        if (showRateField) {
            DropdownSelector(
                label = "Rate Mode",
                options = RateMode.entries,
                selected = rateMode,
                onSelected = { rateMode = it },
            )

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

        if (isImpossibleGoal) {
            Text(
                "This goal is not possible, is ill advised or is possibly unsafe. Please adjust your target weight, rate, or time frame.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Button(
            onClick = {
                val goal =
                    Goal(
                        goalWeight = goalWeight.toDoubleOrNull(),
                        type = goalType,
                        timeMode = timeMode,
                        targetDate = targetDate?.time,
                        ratePerWeek = if (rateMode == RateMode.KG_PER_WEEK) rateInput.toDoubleOrNull() else null,
                        durationWeeks = durationWeeks.toIntOrNull(),
                        rateMode = rateMode,
                        ratePercent = if (rateMode == RateMode.BODYWEIGHT_PERCENT) rateInput.toDoubleOrNull() else null,
                        ratePreset = if (rateMode == RateMode.PRESET) selectedPreset else null,
                        createdAt = System.currentTimeMillis(),
                    )
                viewModel.setGoal(goal)
            },
            enabled =
                !isImpossibleGoal &&
                    (!needsGoalWeight || goalWeight.isNotBlank()) &&
                    (timeMode != GoalTimeMode.BY_DURATION || durationWeeks.isNotBlank()),
        ) {
            Text("Save Goal")
        }

        // === PROJECTIONS ===

        projection.targetCalories?.let {
            Text("Estimated Daily Calorie Target: $it kcal/day", style = MaterialTheme.typography.bodyMedium)
        }
        projection.goalDate?.let { date ->
            Text(
                text = "Estimated Goal Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        projection.finalWeight?.let { weight ->
            Text(
                text = String.format(Locale.UK, "Estimated Final Weight: %.1f kg", weight),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        projection.weightChange?.let { change ->
            Text(
                text = String.format(Locale.UK, "Estimated Weight Change: %.1f kg", change),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (goalType == GoalType.CUT || goalType == GoalType.BULK || goalType == GoalType.TARGET_WEIGHT) {
            projection.dailyCalories?.let {
                Text("Required Daily Calorie Change: $it kcal/day", style = MaterialTheme.typography.bodyMedium)
            }
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
