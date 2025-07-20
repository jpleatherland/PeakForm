package dev.jpleatherland.weighttracker.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jpleatherland.weighttracker.viewmodel.SettingsViewModel
import dev.jpleatherland.weighttracker.viewmodel.WeightUnit
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

@Composable
fun SettingsScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val weightUnit by settingsViewModel.weightUnit.collectAsState()

    val context = LocalContext.current
    val healthPermissions =
        setOf(
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
        )

    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted: Set<String> ->
            Log.d("SettingsScreen", "trying permissions : $granted")
            if (granted.containsAll(healthPermissions)) {
                viewModel.syncHealthConnect()
            } else {
                Toast.makeText(context, "Health Connect permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // --- Unit Selection ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Weight unit:")

            SegmentedButton(
                options = listOf("kg", "lbs"),
                selected = if (weightUnit == WeightUnit.KG) 0 else 1,
                onSelected = {
                    settingsViewModel.setWeightUnit(
                        if (it == 0) WeightUnit.KG else WeightUnit.LB,
                    )
                },
            )
        }

        // --- Health Connect Sync ---
        Button(onClick = {
            permissionLauncher.launch(healthPermissions)
        }) {
            Text("Sync Health Connect")
        }
    }
}

// --- Simple segmented button for two options ---
@Composable
fun SegmentedButton(
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    Row {
        options.forEachIndexed { idx, label ->
            val isSelected = idx == selected
            OutlinedButton(
                onClick = { onSelected(idx) },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (idx < options.size - 1) Modifier.padding(end = 4.dp) else Modifier,
                        ),
            ) {
                Text(
                    label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
