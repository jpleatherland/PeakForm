package dev.jpleatherland.peakform.ui

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import dev.jpleatherland.peakform.util.kgToLb
import dev.jpleatherland.peakform.util.lbToKg
import dev.jpleatherland.peakform.viewmodel.SettingsViewModel
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GoalScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val currentGoal by viewModel.goal.collectAsState()
    val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()
    val maintenance by viewModel.estimatedMaintenanceCalories.collectAsState()
    val weightUnit by settingsViewModel.weightUnit.collectAsState()

    // UI state
    var goalType by remember { mutableStateOf(GoalType.CUT) }
    var goalWeight by remember { mutableStateOf("") }
    var timeMode by remember { mutableStateOf(GoalTimeMode.BY_DURATION) }
    var targetDate by remember { mutableStateOf<Date?>(null) }
    var rateMode by remember { mutableStateOf(RateMode.PRESET) }
    var rateInput by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(RatePreset.LEAN) }
    var durationWeeks by remember { mutableStateOf("") }

    // Sync to existing goal
    LaunchedEffect(currentGoal) {
        currentGoal?.let { goal ->
            goalType = goal.type
            // Always convert stored kg to user unit for display
            goalWeight = goal.goalWeight?.let {
                when (weightUnit) {
                    WeightUnit.KG -> it.toString()
                    WeightUnit.LB -> String.format(Locale.getDefault(), "%.1f", it.kgToLb())
                }
            } ?: ""
            timeMode = goal.timeMode
            targetDate = goal.targetDate?.let { Date(it) }
            durationWeeks = goal.durationWeeks?.toString() ?: ""
            rateMode = goal.rateMode ?: RateMode.PRESET
            selectedPreset = goal.ratePreset ?: RatePreset.LEAN
            rateInput =
                when (rateMode) {
                    RateMode.KG_PER_WEEK ->
                        goal.ratePerWeek?.let {
                            when (weightUnit) {
                                WeightUnit.KG -> it.toString()
                                WeightUnit.LB -> String.format(Locale.getDefault(), "%.2f", it.kgToLb())
                            }
                        } ?: ""
                    RateMode.BODYWEIGHT_PERCENT -> goal.ratePercent?.toString() ?: ""
                    RateMode.PRESET -> "" // No input for preset
                }
        }
    }

    // Current weight: fallback to last, then 70
    val currentWeight =
        avgWeight ?: viewModel.entries.value
            .lastOrNull()
            ?.weight ?: 70.0

    // ========== INPUT LOGIC ==========

    // 1. Should the target weight field be shown?
    val needsGoalWeight = timeMode != GoalTimeMode.BY_DURATION

    // 2. Should the duration field be shown?
    val needsDuration = timeMode == GoalTimeMode.BY_DURATION

    // 3. Should the rate input (or picker) be shown?
    val needsRateInput = timeMode == GoalTimeMode.BY_RATE || timeMode == GoalTimeMode.BY_DURATION

    // 4. Should the date picker be shown?
    val needsDate = timeMode == GoalTimeMode.BY_DATE

    // ========== LIVE PROJECTION (using all UI state) ==========

    // Parse weight from input in user units, store as kg
    fun parseWeightInput(text: String): Double? =
        text.toDoubleOrNull()?.let { value ->
            when (weightUnit) {
                WeightUnit.KG -> value
                WeightUnit.LB -> value.lbToKg()
            }
        }

    // Parse rate input (kg or lb per week)
    fun parseRateInput(text: String): Double? =
        text.toDoubleOrNull()?.let { value ->
            when (weightUnit) {
                WeightUnit.KG -> value
                WeightUnit.LB -> value.lbToKg()
            }
        }

    val projection =
        GoalCalculations.project(
            goal =
                Goal(
                    type = goalType,
                    goalWeight = parseWeightInput(goalWeight),
                    timeMode = timeMode,
                    targetDate = targetDate?.time,
                    rateMode = rateMode,
                    ratePercent = if (rateMode == RateMode.BODYWEIGHT_PERCENT) rateInput.toDoubleOrNull() else null,
                    ratePreset = if (rateMode == RateMode.PRESET) selectedPreset else null,
                    ratePerWeek = if (rateMode == RateMode.KG_PER_WEEK) parseRateInput(rateInput) else null,
                    durationWeeks = durationWeeks.toIntOrNull(),
                ),
            currentWeight = currentWeight,
            avgMaintenance = maintenance,
            rateInput = rateInput,
            selectedPreset = selectedPreset,
            durationWeeks = durationWeeks.toIntOrNull(),
            targetDate = targetDate,
        )

    // ========== IMPOSSIBLE GOAL CHECK ==========
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

    // ========== UI ==========

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DropdownSelector(
            label = "Goal Type",
            options = listOf(GoalType.CUT, GoalType.BULK, GoalType.MAINTAIN),
            selected = goalType,
            onSelected = { goalType = it },
        )

        DropdownSelector(
            label = "How do you want to reach your goal?",
            options = GoalTimeMode.entries,
            selected = timeMode,
            onSelected = { timeMode = it },
        )

        // 1. Target Weight (unless duration mode)
        if (needsGoalWeight) {
            OutlinedTextField(
                value = goalWeight,
                onValueChange = { goalWeight = it },
                label = { Text("Target Weight (${weightUnit.name.lowercase()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        // 2. Date (if by date)
        if (needsDate) {
            val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            OutlinedTextField(
                value = targetDate?.let { dateFormatter.format(it) } ?: "",
                onValueChange = {},
                label = { Text("Target Date") },
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, day)
                                    targetDate = cal.time
                                },
                                Calendar.getInstance().get(Calendar.YEAR),
                                Calendar.getInstance().get(Calendar.MONTH),
                                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
            )
        }

        // 3. Duration (if by duration)
        if (needsDuration) {
            OutlinedTextField(
                value = durationWeeks,
                onValueChange = { durationWeeks = it },
                label = { Text("Duration (weeks)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        // 4. Rate (if by rate or duration)
        if (needsRateInput) {
            DropdownSelector(
                label = "Rate Type",
                options = RateMode.entries,
                selected = rateMode,
                onSelected = { rateMode = it },
                labelForOption = { it.displayName(weightUnit) },
            )
            when (rateMode) {
                RateMode.KG_PER_WEEK -> {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Rate (${weightUnit.name.lowercase()}/week)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }

                RateMode.BODYWEIGHT_PERCENT -> {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Rate (% bodyweight/week)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
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

        // ===== DYNAMIC DERIVED FIELDS (converted to user units where relevant) =====

        // BY DATE: Show required rate when both weight and date are present
        if (
            timeMode == GoalTimeMode.BY_DATE &&
            goalWeight.isNotBlank() &&
            targetDate != null
        ) {
            projection.rateKgPerWeek
                .takeIf { it.isFinite() && it != 0.0 }
                ?.let { rateKg ->
                    val rateDisplay =
                        when (weightUnit) {
                            WeightUnit.KG -> rateKg
                            WeightUnit.LB -> rateKg.kgToLb()
                        }
                    Text(
                        "Required rate: ${String.format(
                            Locale.getDefault(),
                            "%.2f",
                            kotlin.math.abs(rateDisplay),
                        )} ${weightUnit.name.lowercase()}/week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
        }

        // BY RATE: Show derived duration/date/weight change if goalWeight is set and any rate (input or preset) is selected
        if (
            timeMode == GoalTimeMode.BY_RATE &&
            goalWeight.isNotBlank() &&
            (rateInput.isNotBlank() || rateMode == RateMode.PRESET)
        ) {
            if (rateMode == RateMode.PRESET) {
                projection.rateKgPerWeek
                    .takeIf { it.isFinite() && it != 0.0 }
                    ?.let { rateKg ->
                        val rateDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> rateKg
                                WeightUnit.LB -> rateKg.kgToLb()
                            }
                        Text(
                            "Selected rate: ${String.format(
                                Locale.getDefault(),
                                "%.2f",
                                kotlin.math.abs(rateDisplay),
                            )} ${weightUnit.name.lowercase()}/week (${selectedPreset.formatEnumName()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
            } else if (rateInput.isNotBlank()) {
                projection.rateKgPerWeek
                    .takeIf { it.isFinite() && it != 0.0 }
                    ?.let { rateKg ->
                        val rateDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> rateKg
                                WeightUnit.LB -> rateKg.kgToLb()
                            }
                        Text(
                            "Selected rate: ${String.format(
                                Locale.getDefault(),
                                "%.2f",
                                kotlin.math.abs(rateDisplay),
                            )} ${weightUnit.name.lowercase()}/week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
            }
            projection.goalDate
                ?.takeIf { it.time > System.currentTimeMillis() }
                ?.let { date ->
                    Text(
                        "Estimated goal date: ${
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            projection.weightChange
                ?.takeIf { it.isFinite() && it != 0.0 }
                ?.let { changeKg ->
                    val changeDisplay =
                        when (weightUnit) {
                            WeightUnit.KG -> changeKg
                            WeightUnit.LB -> changeKg.kgToLb()
                        }
                    Text(
                        "Total weight change: ${String.format(Locale.getDefault(), "%.1f", changeDisplay)} ${weightUnit.name.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
        }

        // BY DURATION: Show projected final weight, total change, rate, and end date
        if (
            timeMode == GoalTimeMode.BY_DURATION &&
            durationWeeks.isNotBlank() &&
            (rateInput.isNotBlank() || rateMode == RateMode.PRESET)
        ) {
            if (rateMode == RateMode.PRESET) {
                projection.rateKgPerWeek
                    .takeIf { it.isFinite() && it != 0.0 }
                    ?.let { rateKg ->
                        val rateDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> rateKg
                                WeightUnit.LB -> rateKg.kgToLb()
                            }
                        Text(
                            "Selected rate: ${String.format(
                                Locale.getDefault(),
                                "%.2f",
                                kotlin.math.abs(rateDisplay),
                            )} ${weightUnit.name.lowercase()}/week (${selectedPreset.formatEnumName()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
            } else if (rateInput.isNotBlank()) {
                projection.rateKgPerWeek
                    .takeIf { it.isFinite() && it != 0.0 }
                    ?.let { rateKg ->
                        val rateDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> rateKg
                                WeightUnit.LB -> rateKg.kgToLb()
                            }
                        Text(
                            "Selected rate: ${String.format(
                                Locale.getDefault() ,
                                "%.2f",
                                kotlin.math.abs(rateDisplay),
                            )} ${weightUnit.name.lowercase()}/week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
            }
            projection.finalWeight
                ?.takeIf { it.isFinite() }
                ?.let { finalKg ->
                    val finalDisplay =
                        when (weightUnit) {
                            WeightUnit.KG -> finalKg
                            WeightUnit.LB -> finalKg.kgToLb()
                        }
                    Text(
                        "Projected final weight: ${String.format(
                            Locale.getDefault(),
                            "%.1f",
                            finalDisplay,
                        )} ${weightUnit.name.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            projection.weightChange
                ?.takeIf { it.isFinite() && it != 0.0 }
                ?.let { changeKg ->
                    val changeDisplay =
                        when (weightUnit) {
                            WeightUnit.KG -> changeKg
                            WeightUnit.LB -> changeKg.kgToLb()
                        }
                    Text(
                        "Total weight change: ${String.format(Locale.getDefault(), "%.1f", changeDisplay)} ${weightUnit.name.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            projection.goalDate
                ?.takeIf { it.time > System.currentTimeMillis() }
                ?.let { date ->
                    Text(
                        "End date: ${
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
        }

        if (isImpossibleGoal) {
            Text(
                "This goal is not possible, is ill advised, or is possibly unsafe. Please adjust your target, rate, or time frame.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Button(
            onClick = {
                val goal =
                    Goal(
                        goalWeight = parseWeightInput(goalWeight),
                        type = goalType,
                        timeMode = timeMode,
                        targetDate = targetDate?.time,
                        ratePerWeek = if (rateMode == RateMode.KG_PER_WEEK) parseRateInput(rateInput) else null,
                        durationWeeks = durationWeeks.toIntOrNull(),
                        rateMode = rateMode,
                        ratePercent = if (rateMode == RateMode.BODYWEIGHT_PERCENT) rateInput.toDoubleOrNull() else null,
                        ratePreset = if (rateMode == RateMode.PRESET) selectedPreset else null,
                        createdAt = System.currentTimeMillis(),
                    )
                viewModel.setGoal(goal)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            enabled =
                !isImpossibleGoal &&
                    ((needsGoalWeight && goalWeight.isNotBlank()) || !needsGoalWeight) &&
                    ((needsDuration && durationWeeks.isNotBlank()) || !needsDuration) &&
                    ((needsRateInput && (rateInput.isNotBlank() || rateMode == RateMode.PRESET)) || !needsRateInput),
        ) {
            Text("Save Goal")
        }

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Goal Preview", style = MaterialTheme.typography.titleMedium)
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
                            when (timeMode) {
                                GoalTimeMode.BY_DURATION, GoalTimeMode.BY_RATE -> "Goal Date:"
                                GoalTimeMode.BY_DATE -> "Target Goal Date:"
                            }
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault(),
                            ).format(goalDate),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                projection.finalWeight?.let { finalKg ->
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val label =
                            when (timeMode) {
                                GoalTimeMode.BY_DURATION -> "Estimated Final Weight:"
                                GoalTimeMode.BY_DATE, GoalTimeMode.BY_RATE -> "Target Weight:"
                            }
                        val finalDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> finalKg
                                WeightUnit.LB -> finalKg.kgToLb()
                            }
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            String.format("%.1f ${weightUnit.name.lowercase()}", finalDisplay),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                projection.weightChange?.let { changeKg ->
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Total Weight Change:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                        val changeDisplay =
                            when (weightUnit) {
                                WeightUnit.KG -> changeKg
                                WeightUnit.LB -> changeKg.kgToLb()
                            }
                        Text(
                            String.format("%.1f ${weightUnit.name.lowercase()}", changeDisplay),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }
    }
}

fun RateMode.displayName(weightUnit: WeightUnit): String =
    when (this) {
        RateMode.KG_PER_WEEK -> "${if (weightUnit == WeightUnit.KG) "kg" else "lb"}/week"
        RateMode.BODYWEIGHT_PERCENT -> "% bodyweight/week"
        RateMode.PRESET -> "Preset"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    labelForOption: (T) -> String = { it.formatEnumName() },
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            readOnly = true,
            value = labelForOption(selected),
            onValueChange = {}, // No editing
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(labelForOption(option))
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

// Helper function for formatting enum names
fun String.formatEnumName(): String =
    this
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

// Helper function for enum formatting, paste near your other helpers
fun Enum<*>.formatEnumName(): String =
    name
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
