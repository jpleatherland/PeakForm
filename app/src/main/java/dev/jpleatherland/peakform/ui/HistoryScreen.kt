@file:Suppress("ktlint:standard:no-wildcard-imports")

package dev.jpleatherland.peakform.ui

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jpleatherland.peakform.data.WeightEntry
import dev.jpleatherland.peakform.util.formatWeight
import dev.jpleatherland.peakform.util.kgToLb
import dev.jpleatherland.peakform.util.lbToKg
import dev.jpleatherland.peakform.viewmodel.SettingsViewModel
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val entries by viewModel.entries.collectAsState(emptyList())
    val weightUnit by settingsViewModel.weightUnit.collectAsState()
    var editingEntry by remember { mutableStateOf<WeightEntry?>(null) }

    val dateFormat = remember { SimpleDateFormat.getDateInstance() }

    Column(modifier = Modifier.padding(16.dp)) {
        // --- Column Headers ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Date", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Weight", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Calories", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(48.dp)) // For actions
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(entries.size) { index ->
                val entry = entries[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = dateFormat.format(Date(entry.date)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatWeight(entry.weight ?: 0.0, weightUnit),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                        )
                        Text(
                            text = "${entry.calories ?: ""} kcal",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                        )
                        IconButton(
                            onClick = { editingEntry = entry },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Entry",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteWeightEntry(entry) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Entry",
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingEntry != null) {
        EditEntryDialog(
            original = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                viewModel.updateEntry(updated)
                editingEntry = null
            },
            weightUnit = weightUnit, // Pass the unit here!
        )
    }
}

@Composable
fun EditEntryDialog(
    original: WeightEntry,
    onDismiss: () -> Unit,
    onSave: (WeightEntry) -> Unit,
    weightUnit: WeightUnit,
) {
    val dateFormat =
        remember {
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        }
    val decimalFormat =
        remember {
            DecimalFormat.getInstance(Locale.getDefault()) as DecimalFormat
        }

    // Display the original weight in the user's preferred unit
    val safeWeightKg = original.weight ?: 0.0
    val displayWeight =
        when (weightUnit) {
            WeightUnit.KG -> safeWeightKg
            WeightUnit.LB -> safeWeightKg.kgToLb()
        }
    val safeCalories = original.calories ?: 0

    var weightInput by remember { mutableStateOf(decimalFormat.format(displayWeight)) }
    var caloriesInput by remember { mutableStateOf(decimalFormat.format(safeCalories)) }

    // Parse user input in current unit, then convert to kg for storage
    val parsedWeightUserUnit =
        try {
            decimalFormat.parse(weightInput)?.toDouble() ?: displayWeight
        } catch (e: Exception) {
            displayWeight
        }
    val parsedWeightKg =
        when (weightUnit) {
            WeightUnit.KG -> parsedWeightUserUnit
            WeightUnit.LB -> parsedWeightUserUnit.lbToKg()
        }

    val parsedCalories =
        try {
            decimalFormat.parse(caloriesInput)?.toInt() ?: safeCalories
        } catch (e: Exception) {
            safeCalories
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Entry",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = dateFormat.format(Date(original.date)),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { input -> weightInput = input },
                    label = { Text("Weight (${weightUnit.name.lowercase()})") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { input -> caloriesInput = input },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newWeightSource =
                        if (parsedWeightKg != (original.weight ?: 0.0)) "user" else original.weightSource
                    val newCaloriesSource =
                        if (parsedCalories != original.calories) "user" else original.caloriesSource
                    onSave(
                        original.copy(
                            weight = parsedWeightKg, // Always kg!
                            calories = parsedCalories,
                            weightSource = newWeightSource,
                            caloriesSource = newCaloriesSource,
                        ),
                    )
                },
                enabled = weightInput.isNotBlank() && caloriesInput.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
