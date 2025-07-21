@file:Suppress("ktlint:standard:no-wildcard-imports")

package dev.jpleatherland.weighttracker.ui

import android.icu.text.NumberFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: WeightViewModel) {
    val entries by viewModel.entries.collectAsState(emptyList())
    var editingEntry by remember { mutableStateOf<WeightEntry?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries.size) { index ->
                val entry = entries[index]
                val dateFormat = SimpleDateFormat.getDateInstance()
                val numberFormat = NumberFormat.getNumberInstance()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateFormat.format(Date(entry.date)),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = numberFormat.format(entry.weight ?: 0.0),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${entry.calories} kcal",
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "✏️",
                        modifier =
                            Modifier
                                .clickable { editingEntry = entry }
                                .padding(8.dp),
                    )
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
        )
    }
}

@Composable
fun EditEntryDialog(
    original: WeightEntry,
    onDismiss: () -> Unit,
    onSave: (WeightEntry) -> Unit,
) {
    val context = LocalContext.current

    val calendar =
        remember(original.date) {
            Calendar.getInstance().apply { time = Date(original.date) }
        }

    var selectedDate by remember { mutableStateOf(original.date) }
    var calories by remember { mutableStateOf(original.calories.toString()) }

    val safeWeight = original.weight ?: 0.0
    var weightWhole by remember { mutableStateOf(safeWeight.toInt() ?: 0) }
    var weightDecimal by remember { mutableStateOf(((safeWeight * 10) % 10).toInt()) }

    val weight =
        remember(weightWhole, weightDecimal) {
            (weightWhole + weightDecimal / 10.0)
        }

    val dateFormat = SimpleDateFormat.getDateInstance()
    val numberFormat = NumberFormat.getNumberInstance()

    var weightInput by remember {
        mutableStateOf(numberFormat.format(safeWeight))
    }

    LaunchedEffect(weightWhole, weightDecimal) {
        val combined = weightWhole + weightDecimal / 10.0
        weightInput = numberFormat.format(combined)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val updated =
                    original.copy(
                        date = selectedDate,
                        weight = weight,
                        calories = calories.toIntOrNull() ?: original.calories,
                    )
                onSave(updated)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Entry for ${SimpleDateFormat("dd MMM yyyy", Locale.UK).format(selectedDate)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weight Picker
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { input ->
                        val cleaned =
                            input
                                .replace(",", ".") // Convert comma to dot if user is in comma locale
                                .filter { it.isDigit() || it == '.' } // Allow digits and dots only
                                .replace(Regex("""(\..*?)\..*"""), "$1") // Only allow one dot
                                .takeIf { it.count { c -> c == '.' } <= 1 } ?: ""

                        // Limit to 2 digits after decimal
                        val formatted = cleaned.replace(Regex("""^(\d+)(\.\d{0,2})?.*"""), "$1$2")

                        weightInput = formatted

                        formatted.toDoubleOrNull()?.let { newWeight ->
                            weightWhole = newWeight.toInt()
                            weightDecimal = ((newWeight * 10) % 10).toInt()
                        }
                    },
                    label = { Text("Weight") },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                        ),
                    singleLine = true,
                )

                // Calories Input
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
    )
}
