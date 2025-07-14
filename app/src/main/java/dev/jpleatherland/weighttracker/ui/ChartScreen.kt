package dev.jpleatherland.weighttracker.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.jpleatherland.weighttracker.data.WeightEntry
import java.text.SimpleDateFormat
import java.util.*

import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

@Composable
fun ChartLayout(entries: List<WeightEntry>) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val chartModifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)

    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Weight Over Time", style = MaterialTheme.typography.titleMedium)
            WeightChart(
                entries = entries, modifier = chartModifier
                    .height(200.dp)
                    .fillMaxWidth()
            )

            Text("Calories Over Time", style = MaterialTheme.typography.titleMedium)
            CaloriesChart(
                entries = entries, modifier = chartModifier
                    .height(200.dp)
                    .fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                WeightChart(
                    entries = entries, modifier = Modifier
                        .fillMaxSize()
                        .height(300.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                CaloriesChart(
                    entries = entries, modifier = Modifier
                        .fillMaxSize()
                        .height(300.dp)
                )
            }
        }
    }
}

@Composable
fun WeightChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    AndroidView(factory = { context ->
        LineChart(context).apply {
            val chartHeightDp = 300.dp
            val heightPx = with(density) { chartHeightDp.roundToPx() }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
            setTouchEnabled(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = object : ValueFormatter() {
                // not fussed about Locale.getDefault() being stale if Locale changed while app is in use
                // it'll all be fine on app restart
                // and won't break in the mean time
                @SuppressLint("ConstantLocale")
                private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return sdf.format(Date(value.toLong()))
                }
            }
        }
    }, update = { chart ->
        val dataPoints = entries
            .filter { it.weight != null }
            .sortedBy { it.date }
            .map {
                Entry(it.date.toFloat(), it.weight!!.toFloat())
            }

        val lineDataSet = LineDataSet(dataPoints, "Weight (kg)").apply {
            color = Color.BLUE
            valueTextColor = Color.DKGRAY
            setDrawCircles(true)
            setDrawValues(false)
        }

        chart.data = LineData(lineDataSet)
        chart.invalidate()
    })
}

@Composable
fun CaloriesChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    AndroidView(factory = { context ->
        LineChart(context).apply {
            val chartHeightDp = 300.dp
            val heightPx = with(density) { chartHeightDp.roundToPx() }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
            setTouchEnabled(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = object : ValueFormatter() {
                // not fussed about Locale.getDefault() being stale if Locale changed while app is in use
                // it'll all be fine on app restart
                // and won't break in the mean time
                @SuppressLint("ConstantLocale")
                private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    return sdf.format(Date(value.toLong()))
                }
            }
        }
    }, update = { chart ->
        val dataPoints = entries
            .filter { it.calories != null }
            .sortedBy { it.date }
            .map {
                Entry(it.date.toFloat(), it.calories!!.toFloat())
            }
        val lineDataSet = LineDataSet(dataPoints, "Calories (kcal)").apply {
            color = Color.RED
            valueTextColor = Color.DKGRAY
            setDrawCircles(true)
            setDrawValues(false)
        }
        chart.data = LineData(lineDataSet)
        chart.invalidate()
    })
}

@Composable
fun ChartScreen(viewModel: WeightViewModel) {
    val entries by viewModel.entries.collectAsState()
    ChartLayout(entries = entries)
}