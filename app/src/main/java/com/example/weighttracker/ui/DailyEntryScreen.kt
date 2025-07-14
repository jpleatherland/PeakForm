package com.example.weighttracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.weighttracker.data.WeightEntry
import com.example.weighttracker.viewmodel.WeightViewModel
import java.util.Locale

@Composable
fun DailyEntryScreen(viewModel: WeightViewModel) {
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    val entries by viewModel.entries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Enter weight (kg)") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            label = { Text("Enter calories") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Entry")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Logged Entries:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                EntryRow(entry)
            }
        }
    }
}
@Composable
fun EntryRow(entry: WeightEntry) {
    val date = remember(entry.date) {
        java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(java.util.Date(entry.date))
    }

    val weightText = entry.weight?.let { "Weight: $it kg" } ?: ""
    val calText = entry.calories?.let { "Calories: $it kcal" } ?: ""

    Text(
        text = "$date - $weightText $calText".trim(),
        style = MaterialTheme.typography.bodyMedium
    )
}