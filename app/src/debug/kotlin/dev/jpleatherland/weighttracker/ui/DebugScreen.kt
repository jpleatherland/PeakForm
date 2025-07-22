package dev.jpleatherland.weighttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jpleatherland.weighttracker.data.GoalDao
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.dev.*

@Composable
fun DebugScreen(
    dao: WeightDao,
    goalDao: GoalDao,
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text("Debug Tools", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Generate Test Data:")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                generateTestData(scope, dao, goalDao, TestDataType.TRENDING_UP)
            }) {
                Text("Trend Up")
            }
            Button(onClick = {
                generateTestData(scope, dao, goalDao, TestDataType.TRENDING_DOWN)
            }) {
                Text("Trend Down")
            }
            Button(onClick = {
                generateTestData(scope, dao, goalDao, TestDataType.CUT_WITH_JUMP)
            }) {
                Text("Cut with Jump")
            }
            Button(onClick = {
                generateTestData(scope, dao, goalDao, TestDataType.BULK_WITH_JUMP)
            }) {
                Text("Bulk with Jump")
            }
            Button(onClick = {
                generateTestData(scope, dao, goalDao, TestDataType.RANDOM)
            }) {
                Text("Random")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                wipeAllData(scope, dao)
                wipeGoalData(scope, goalDao)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Wipe All Data", color = MaterialTheme.colorScheme.onError)
        }
    }
}
