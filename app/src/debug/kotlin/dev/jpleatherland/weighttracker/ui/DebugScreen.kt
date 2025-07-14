package dev.jpleatherland.weighttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jpleatherland.weighttracker.data.WeightDao
import dev.jpleatherland.weighttracker.dev.*
import kotlinx.coroutines.launch

@Composable
fun DebugScreen(dao: WeightDao) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Debug Tools", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Generate Test Data:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                generateTestData(scope, dao, TestDataType.TRENDING_UP)
            }) {
                Text("Trend Up")
            }
            Button(onClick = {
                generateTestData(scope, dao, TestDataType.TRENDING_DOWN)
            }) {
                Text("Trend Down")
            }
            Button(onClick = {
                generateTestData(scope, dao, TestDataType.RANDOM)
            }) {
                Text("Random")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { wipeAllData(scope, dao) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Wipe All Data", color = MaterialTheme.colorScheme.onError)
        }
    }
}
