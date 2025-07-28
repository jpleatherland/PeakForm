package dev.jpleatherland.peakform.ui

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
import dev.jpleatherland.peakform.viewmodel.SettingsViewModel
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import dev.jpleatherland.peakform.viewmodel.WeightViewModel

@Composable
fun SettingsScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val weightUnit by settingsViewModel.weightUnit.collectAsState()
    val isSyncing by viewModel.syncInProgress.collectAsState()
    val context = LocalContext.current
    val readPermissions =
        setOf(
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
        )
    val writePermissions =
        setOf(
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class),
        )

    var showWriteDialog by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted: Set<String> ->
            Log.d("SettingsScreen", "trying permissions : $granted")
            if (showWriteDialog && granted.containsAll(writePermissions)) {
                viewModel.writeAppOnlyEntriesToHealthConnect()
                showWriteDialog = false
            } else if (granted.containsAll(readPermissions)) {
                viewModel.readHealthConnect()
            } else {
                Toast.makeText(context, "Health Connect permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
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
                    settingsViewModel.setWeightUnit(if (it == 0) WeightUnit.KG else WeightUnit.LB)
                },
            )
        }

        // --- Health Connect Sync: READ ---
        Button(
            onClick = {
                permissionLauncher.launch(
                    readPermissions,
                )
            },
            enabled = !isSyncing,
        ) { Text("Read from Health Connect") }

        // --- Health Connect Sync: WRITE ---
        Button(
            onClick = { showWriteDialog = true },
            enabled = !isSyncing,
        ) { Text("Write to Health Connect") }
    }

    // --- Popup/AlertDialog for Write Confirmation ---
    if (showWriteDialog) {
        AlertDialog(
            onDismissRequest = { showWriteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionLauncher.launch(
                            writePermissions,
                        )
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showWriteDialog = false }) { Text("Cancel") }
            },
            title = { Text("Sync user data to Health Connect?") },
            text = {
                Text(
                    """This will write only data that you have entered in PeakForm (not imported from Health Connect) to your Health Connect profile. 
                        |Data previously imported from Health Connect will remain unchanged.
                    """.trimMargin(),
                )
            },
        )
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
