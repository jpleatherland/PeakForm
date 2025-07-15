package dev.jpleatherland.weighttracker.ui

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

@Composable
fun DailyEntryScreen(viewModel: WeightViewModel) {
    var weight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    val entries by viewModel.entries.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
        val avgWeight by viewModel.sevenDayAvgWeight.collectAsState()

        val numberFormat = NumberFormat.getNumberInstance()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                        Text(
                            text = numberFormat.format(it),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }
    }
}
