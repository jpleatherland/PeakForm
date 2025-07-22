package dev.jpleatherland.weighttracker.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dev.jpleatherland.weighttracker.data.Goal
import dev.jpleatherland.weighttracker.data.GoalSegment
import dev.jpleatherland.weighttracker.data.WeightEntry
import dev.jpleatherland.weighttracker.util.GoalProjection
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ChartLayout(
    entries: List<WeightEntry>,
    goal: Goal?,
    segments: List<GoalSegment>,
    projection: GoalProjection?,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val chartModifier =
        Modifier
            .fillMaxWidth()
            .padding(8.dp)

    if (isPortrait) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Weight Over Time", style = MaterialTheme.typography.titleMedium)
            WeightChart(
                entries = entries,
                goal = goal,
                segments = segments,
                projection = projection,
                modifier =
                    chartModifier
                        .height(200.dp)
                        .fillMaxWidth(),
            )

            Text("Calories Over Time", style = MaterialTheme.typography.titleMedium)
            CaloriesChart(
                entries = entries,
                modifier =
                    chartModifier
                        .height(200.dp)
                        .fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                WeightChart(
                    entries = entries,
                    goal = goal,
                    segments = segments,
                    projection = projection,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .height(300.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                CaloriesChart(
                    entries = entries,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .height(300.dp),
                )
            }
        }
    }
}

@Composable
fun WeightChart(
    entries: List<WeightEntry>,
    goal: Goal?,
    segments: List<GoalSegment>,
    projection: GoalProjection?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    AndroidView(factory = { context ->
        LineChart(context).apply {
            val chartHeightDp = 300.dp
            val heightPx = with(density) { chartHeightDp.roundToPx() }
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx,
                )
            setTouchEnabled(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter =
                object : ValueFormatter() {
                    private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

                    override fun getFormattedValue(value: Float): String = sdf.format(Date(TimeUnit.DAYS.toMillis(value.toLong())))
                }
        }
    }, update = { chart ->

        // --- ACTUAL WEIGHT ENTRIES ---
        val dataPoints =
            entries
                .filter { it.weight != null }
                .sortedBy { it.date }
                .map {
                    Entry(TimeUnit.MILLISECONDS.toDays(it.date).toFloat(), it.weight!!.toFloat())
                }

        val lineDataSet =
            LineDataSet(dataPoints, "Weight (kg)").apply {
                color = Color.BLUE
                valueTextColor = Color.DKGRAY
                setDrawCircles(true)
                setDrawValues(false)
            }

        // Trend line calculation (linear regression through actual data)
        val trendPoints = calculateTrendLine(dataPoints)

        val trendDataSet =
            LineDataSet(trendPoints, "Actual Trend").apply {
                color = Color.GRAY
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                setDrawHighlightIndicators(false)
            }

        // --- GOAL PROJECTION/SEGMENTED TREND LINE ---
        val goalProjectionPoints =
            if (goal != null && projection != null) {
                buildGoalProjectionEntries(goal, segments, entries, projection)
            } else {
                emptyList()
            }

        val projectionDataSet =
            LineDataSet(goalProjectionPoints, "Goal Projection").apply {
                color = Color.GREEN
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                setDrawHighlightIndicators(false)
            }

        val dataSets = mutableListOf<ILineDataSet>(lineDataSet, projectionDataSet, trendDataSet)
        chart.data = LineData(dataSets)
        chart.invalidate()
    })
}

fun buildGoalProjectionEntries(
    goal: Goal,
    segments: List<GoalSegment>,
    entries: List<WeightEntry>,
    projection: GoalProjection?,
): List<Entry> {
    if (projection == null) return emptyList()

    val firstWeightEntry = entries.firstOrNull { it.weight != null }
    val startWeight = firstWeightEntry?.weight ?: return emptyList()
    val startDate = firstWeightEntry.date

    val allSegments = segments.sortedBy { it.startDate }
    val entriesList = mutableListOf<Entry>()

    fun toChartX(millis: Long) = TimeUnit.MILLISECONDS.toDays(millis).toFloat()

    entriesList.add(Entry(toChartX(startDate), startWeight.toFloat()))

    if (allSegments.isEmpty()) {
        val endDate = projection.goalDate?.time ?: goal.targetDate ?: (startDate + TimeUnit.DAYS.toMillis(90))
        val endWeight = projection.finalWeight ?: goal.goalWeight ?: startWeight
        entriesList.add(Entry(toChartX(endDate), endWeight.toFloat()))
        return entriesList
    }

    var prevDate = startDate
    var prevWeight = startWeight

    for (segment in allSegments) {
        // To segment start
        if (segment.startDate > prevDate) {
            entriesList.add(Entry(toChartX(segment.startDate), segment.startWeight.toFloat()))
        }
        // To segment end
        entriesList.add(Entry(toChartX(segment.endDate), segment.endWeight.toFloat()))
        prevDate = segment.endDate
        prevWeight = segment.endWeight
    }

    val finalTargetDate =
        projection.goalDate?.time ?: goal.targetDate ?: (prevDate + TimeUnit.DAYS.toMillis(90))
    val finalTargetWeight = projection.finalWeight ?: goal.goalWeight ?: prevWeight

    if (finalTargetDate > prevDate) {
        entriesList.add(Entry(toChartX(finalTargetDate), finalTargetWeight.toFloat()))
    }

    return entriesList
}

// Helper for linear regression
private fun calculateTrendLine(entries: List<Entry>): List<Entry> {
    if (entries.size < 2) return entries // Not enough for regression

    val n = entries.size
    val sumX = entries.sumOf { it.x.toDouble() }
    val sumY = entries.sumOf { it.y.toDouble() }
    val sumXY = entries.sumOf { (it.x * it.y).toDouble() }
    val sumX2 = entries.sumOf { (it.x * it.x).toDouble() }

    val b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    val a = (sumY - b * sumX) / n

    return entries.map { entry ->
        Entry(entry.x, (a + b * entry.x).toFloat())
    }
}

@Composable
fun CaloriesChart(
    entries: List<WeightEntry>,
    goalCalories: Int? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    AndroidView(factory = { context ->
        LineChart(context).apply {
            val chartHeightDp = 300.dp
            val heightPx = with(density) { chartHeightDp.roundToPx() }
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx,
                )
            setTouchEnabled(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter =
                object : ValueFormatter() {
                    @SuppressLint("ConstantLocale")
                    private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

                    override fun getFormattedValue(value: Float): String = sdf.format(Date(value.toLong()))
                }
        }
    }, update = { chart ->
        val dataPoints =
            entries
                .filter { it.calories != null }
                .sortedBy { it.date }
                .map {
                    Entry(it.date.toFloat(), it.calories!!.toFloat())
                }

        // Trend line calculation (reuse the same helper function!)
        val trendPoints = calculateTrendLine(dataPoints)

        val goalDataSet =
            goalCalories?.let { gc ->
                if (dataPoints.isNotEmpty()) {
                    val goalEntries =
                        listOf(
                            Entry(dataPoints.first().x, gc.toFloat()),
                            Entry(dataPoints.last().x, gc.toFloat()),
                        )
                    LineDataSet(
                        goalEntries,
                        "Goal",
                    ).apply {
                        color = Color.GREEN
                        lineWidth = 2f
                        setDrawCircles(false)
                        setDrawValues(false)
                        enableDashedLine(10f, 5f, 0f)
                        setDrawHighlightIndicators(false)
                    }
                } else {
                    null
                }
            }

        val lineDataSet =
            LineDataSet(dataPoints, "Calories (kcal)").apply {
                color = Color.RED
                valueTextColor = Color.DKGRAY
                setDrawCircles(true)
                setDrawValues(false)
            }

        val trendDataSet =
            LineDataSet(trendPoints, "Trend").apply {
                color = Color.GRAY
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                setDrawHighlightIndicators(false)
            }

        val dataSets = mutableListOf(lineDataSet, trendDataSet)
        goalDataSet?.let { dataSets.add(it) }
        chart.data = LineData(dataSets as List<ILineDataSet>?)
        chart.invalidate()
    })
}

@Composable
fun ChartScreen(viewModel: WeightViewModel) {
    val entries by viewModel.entries.collectAsState()
    val goal by viewModel.goal.collectAsState()

    val segments by viewModel.goalSegments.collectAsState()
    val goalProjection by viewModel.goalProjection.collectAsState()

    Log.d("ChartScreen", "Segments count: ${segments.size}")
    val entriesAsc = entries.sortedBy { it.date }

    ChartLayout(entries = entriesAsc, goal = goal, segments = segments, projection = goalProjection)
}
