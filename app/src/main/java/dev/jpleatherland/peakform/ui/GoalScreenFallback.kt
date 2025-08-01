package dev.jpleatherland.peakform.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import kotlin.math.roundToInt

@Composable
fun GoalScreenFallback(
    onTDEEConfirmed: (Int) -> Unit,
    onStartWeightEntered: (Double) -> Unit,
) {
    var tdeeInputs by remember { mutableStateOf(TDEEInputs()) }

    val tdeeEstimate = calculateTDEE(tdeeInputs)
    val canConfirm = tdeeEstimate != null && tdeeInputs.weight.toDoubleOrNull() != null

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        GoalSetupStatusCard(startWeightAvailable = false, maintenanceAvailable = false)

        Spacer(Modifier.height(16.dp))

        Text("Don't have enough data? Estimate your maintenance:", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        TDEECalculatorCard(
            inputs = tdeeInputs,
            onInputsChange = { tdeeInputs = it },
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "We do not store your personal data. Inputs are used once to estimate your maintenance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Estimated TDEE: ${tdeeEstimate?.let { "$it kcal" } ?: "N/A"}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                tdeeEstimate?.let { tdee ->
                    onTDEEConfirmed(tdee)
                    onStartWeightEntered(tdeeInputs.weight.toDouble())
                }
            },
            enabled = canConfirm,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Use this estimate")
        }
    }
}

@Composable
fun TDEECalculatorCard(
    inputs: TDEEInputs,
    onInputsChange: (TDEEInputs) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = inputs.age,
            onValueChange = { onInputsChange(inputs.copy(age = it)) },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Units")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Metric")
            RadioButton(
                selected = inputs.heightUnit == HeightUnit.CM,
                onClick = {
                    onInputsChange(
                        inputs.copy(
                            heightUnit = HeightUnit.CM,
                            weightUnit = WeightUnit.KG,
                        ),
                    )
                },
            )
            Spacer(Modifier.width(8.dp))
            Text("Imperial")
            RadioButton(
                selected = inputs.heightUnit == HeightUnit.INCHES,
                onClick = {
                    onInputsChange(
                        inputs.copy(
                            heightUnit = HeightUnit.INCHES,
                            weightUnit = WeightUnit.LB,
                        ),
                    )
                },
            )
        }
        OutlinedTextField(
            value = inputs.height,
            onValueChange = { onInputsChange(inputs.copy(height = it)) },
            label = {
                Text("Height (${if (inputs.heightUnit == HeightUnit.CM) "cm" else "inches"})")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = inputs.weight,
            onValueChange = { onInputsChange(inputs.copy(weight = it)) },
            label = {
                Text("Weight (${if (inputs.weightUnit == WeightUnit.KG) "kg" else "lb"})")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Text("Sex")
        Row {
            RadioButton(
                selected = inputs.sex == Sex.MALE,
                onClick = { onInputsChange(inputs.copy(sex = Sex.MALE)) },
            )
            Text("Male")
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = inputs.sex == Sex.FEMALE,
                onClick = { onInputsChange(inputs.copy(sex = Sex.FEMALE)) },
            )
            Text("Female")
        }

        Spacer(Modifier.height(8.dp))
        Text("Activity Level")
        ActivityLevel.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = inputs.activityLevel == level,
                    onClick = { onInputsChange(inputs.copy(activityLevel = level)) },
                )
                Text(level.label)
            }
        }
    }
}

@Composable
fun GoalSetupStatusCard(
    startWeightAvailable: Boolean,
    maintenanceAvailable: Boolean,
) {
    val allGood = startWeightAvailable && maintenanceAvailable

    val containerColor =
        if (allGood) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

    val contentColor =
        if (allGood) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (allGood) "You're ready to create a goal!" else "Not enough data to create a goal",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (startWeightAvailable) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text =
                        if (startWeightAvailable) {
                            "✓ Starting weight available"
                        } else {
                            """• Log at least one weight entry. 
                                |For best results, log at least a week of weights.
                            """.trimMargin()
                        },
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (maintenanceAvailable) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text =
                        if (maintenanceAvailable) {
                            "✓ Maintenance calories estimated"
                        } else {
                            """• Log at least two entries with calories over a 7 day period. 
                                |For best results log at least 14 days of weight and calories.
                            """.trimMargin()
                        },
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

fun calculateTDEE(inputs: TDEEInputs): Int? {
    val age = inputs.age.toIntOrNull() ?: return null
    val height = inputs.height.toDoubleOrNull() ?: return null
    val weight = inputs.weight.toDoubleOrNull() ?: return null

    val heightCm = if (inputs.heightUnit == HeightUnit.INCHES) height * 2.54 else height
    val weightKg = if (inputs.weightUnit == WeightUnit.LB) weight * 0.453592 else weight

    val bmr =
        when (inputs.sex) {
            Sex.MALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            Sex.FEMALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            null -> return null
        }

    return (bmr * inputs.activityLevel.multiplier).roundToInt()
}

// Enums and data classes
enum class Sex { MALE, FEMALE }

enum class ActivityLevel(
    val label: String,
    val multiplier: Double,
) {
    SEDENTARY("Sedentary", 1.2),
    LIGHT("Light", 1.375),
    MODERATE("Moderate", 1.55),
    ACTIVE("Active", 1.725),
    VERY_ACTIVE("Very Active", 1.9),
}

enum class HeightUnit { CM, INCHES }

data class TDEEInputs(
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val sex: Sex? = null,
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val heightUnit: HeightUnit = HeightUnit.CM,
    val weightUnit: WeightUnit = WeightUnit.KG,
)
