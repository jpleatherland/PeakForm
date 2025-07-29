package dev.jpleatherland.peakform.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontSynthesis.Companion.Weight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dev.jpleatherland.peakform.R
import dev.jpleatherland.peakform.data.Goal
import dev.jpleatherland.peakform.data.GoalSegment
import dev.jpleatherland.peakform.data.WeightEntry
import dev.jpleatherland.peakform.util.GoalProjection
import dev.jpleatherland.peakform.util.WeightUnitValueFormatter
import dev.jpleatherland.peakform.util.kgToLb
import dev.jpleatherland.peakform.viewmodel.SettingsViewModel
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import dev.jpleatherland.peakform.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ChartLayout(
    entries: List<WeightEntry>,
    goal: Goal?,
    segments: List<GoalSegment>,
    projection: GoalProjection?,
    weightUnit: WeightUnit,
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
                weightUnit = weightUnit,
                modifier =
                    chartModifier
                        .height(200.dp)
                        .fillMaxWidth(),
            )

            Text("Calories Over Time", style = MaterialTheme.typography.titleMedium)
            CaloriesChart(
                entries = entries,
                goal = goal,
                segments = segments,
                projection = projection,
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
                    weightUnit = weightUnit,
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
                    goal = goal,
                    segments = segments,
                    projection = projection,
                    modifier =
                        chartModifier
                            .height(200.dp)
                            .fillMaxWidth(),
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
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AndroidView(factory = { context ->
        val markerView = ChartLabels(context, R.layout.marker_view, R.string.marker_weight_date, weightUnit)
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
            axisLeft.valueFormatter = WeightUnitValueFormatter(weightUnit)
            axisRight.valueFormatter = WeightUnitValueFormatter(weightUnit)
            marker = markerView
        }
    }, update = { chart ->

        // --- ACTUAL WEIGHT ENTRIES ---
        val dataPoints =
            entries
                .filter { it.weight != null }
                .sortedBy { it.date }
                .map {
                    val x = TimeUnit.MILLISECONDS.toDays(it.date).toFloat()
                    val y =
                        when (weightUnit) {
                            WeightUnit.KG -> it.weight!!.toFloat()
                            WeightUnit.LB -> it.weight!!.kgToLb().toFloat()
                        }
                    val dateString = dateFormatter.format(Date(it.date))
                    Entry(x, y).apply {
                        data = "type:weight|$dateString"
                    }
                }

        val lineDataSet =
            LineDataSet(
                dataPoints,
                when (weightUnit) {
                    WeightUnit.KG -> "Weight (kg)"
                    WeightUnit.LB -> "Weight (lb)"
                },
            ).apply {
                color = Color.BLUE
                valueTextColor = Color.DKGRAY
                setDrawCircles(true)
                setDrawValues(false)
            }

        // Trend line calculation (linear regression through actual data)
        val trendPoints =
            calculateTrendLine(dataPoints)

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
                    .map { entry ->
                        Entry(
                            entry.x,
                            when (weightUnit) {
                                WeightUnit.KG -> entry.y
                                WeightUnit.LB -> entry.y * 2.20462f
                            },
                        )
                    }
            } else {
                emptyList()
            }
        val projectionDataSet =
            LineDataSet(
                goalProjectionPoints,
                when (weightUnit) {
                    WeightUnit.KG -> "Goal Projection (kg)"
                    WeightUnit.LB -> "Goal Projection (lb)"
                },
            ).apply {
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
    val startDate = goal.createdAt

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
    goal: Goal?,
    segments: List<GoalSegment>,
    projection: GoalProjection?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AndroidView(factory = { context ->
        val markerView = ChartLabels(context, R.layout.marker_view, R.string.marker_calories_date)
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

                    override fun getFormattedValue(value: Float): String = sdf.format(Date(TimeUnit.DAYS.toMillis(value.toLong())))
                }
            marker = markerView
        }
    }, update = { chart ->
        // 1. Plot calorie data points
        val dataPoints =
            entries
                .filter { it.calories != null }
                .sortedBy { it.date }
                .map {
                    val x = TimeUnit.MILLISECONDS.toDays(it.date).toFloat()
                    val y = it.calories!!.toFloat()
                    val dateString = dateFormatter.format(Date(it.date))
                    Entry(x, y).apply {
                        data = "type:calories|$dateString"
                    }
                }

        // 2. Trend line
        val trendPoints = calculateTrendLine(dataPoints)

        // 3. Stepped goal line (green, dashed)
        val goalCalories =
            projection?.targetCalories

        val segCals = segments.map { it.targetCalories }
        Log.d("CaloriesChart", "segCals: $segCals")
        val goalCaloriesEntries =
            buildGoalCaloriesEntries(
                goal = goal,
                segments = segments,
                entries = entries,
                fallbackGoalCalories = goalCalories,
            )
        val goalDataSet =
            if (goalCaloriesEntries.size > 1) {
                LineDataSet(goalCaloriesEntries, "Goal Calories").apply {
                    color = Color.GREEN
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    enableDashedLine(10f, 5f, 0f)
                    setDrawHighlightIndicators(false)
                    isHighlightEnabled = false
                }
            } else {
                null
            }

        // 4. Entries line (red)
        val lineDataSet =
            LineDataSet(dataPoints, "Calories (kcal)").apply {
                color = Color.RED
                valueTextColor = Color.DKGRAY
                setDrawCircles(true)
                setDrawValues(false)
            }

        // 5. Trend line (gray, dashed)
        val trendDataSet =
            LineDataSet(trendPoints, "Trend").apply {
                color = Color.GRAY
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
                setDrawHighlightIndicators(false)
            }

        // 6. Add datasets
        val dataSets = mutableListOf<ILineDataSet>(lineDataSet, trendDataSet)
        goalDataSet?.let { dataSets.add(it) }
        chart.data = LineData(dataSets)
        chart.invalidate()
    })
}

// Helper: Stepped goal calories line
fun buildGoalCaloriesEntries(
    goal: Goal?,
    segments: List<GoalSegment>,
    entries: List<WeightEntry>,
    fallbackGoalCalories: Int? = null,
): List<Entry> {
    if (entries.isEmpty() && fallbackGoalCalories == null) return emptyList()
    val toChartX = { millis: Long -> TimeUnit.MILLISECONDS.toDays(millis).toFloat() }

    val chartStartX = goal?.createdAt
    val chartEndX = entries.maxByOrNull { it.date }?.let { toChartX(it.date) }
    val out = mutableListOf<Entry>()
    val segs = segments.sortedBy { it.startDate }

    // No segments: fallback flat line
    if (segs.isEmpty()) {
        if (fallbackGoalCalories != null && chartStartX != null && chartEndX != null) {
            out.add(Entry(toChartX(chartStartX), fallbackGoalCalories.toFloat()))
            out.add(Entry(chartEndX, fallbackGoalCalories.toFloat()))
        }
        return out
    }

    val firstSeg = segs.first()
    val lastSeg = segs.last()

    // Before first segment: fallback
    if (chartStartX != null && chartStartX < toChartX(firstSeg.startDate)) {
        val preSegCal = fallbackGoalCalories?.toFloat() ?: firstSeg.targetCalories.toFloat()
        out.add(Entry(toChartX(chartStartX), preSegCal))
        out.add(Entry(toChartX(firstSeg.startDate), preSegCal))
    }

    // Each segment: stepped
    for ((index, seg) in segs.withIndex()) {
        val segStartX = toChartX(seg.startDate)
        val segEndX =
            if (index < segs.size - 1) {
                toChartX(segs[index + 1].startDate)
            } else {
                toChartX(seg.endDate)
            }
        out.add(Entry(segStartX, seg.targetCalories.toFloat()))
        out.add(Entry(segEndX, seg.targetCalories.toFloat()))
    }

    // After last segment: fallback
    if (chartEndX != null && chartEndX > toChartX(lastSeg.endDate)) {
        val postSegCal = fallbackGoalCalories?.toFloat() ?: lastSeg.targetCalories.toFloat()
        out.add(Entry(toChartX(lastSeg.endDate), postSegCal))
        out.add(Entry(chartEndX, postSegCal))
    }

    return out
}

@Composable
fun ChartScreen(
    viewModel: WeightViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val entries by viewModel.entries.collectAsState()
    val goal by viewModel.goal.collectAsState()

    val segments by viewModel.goalSegments.collectAsState()
    val goalProjection by viewModel.goalProjection.collectAsState()

    val weightUnit by settingsViewModel.weightUnit.collectAsState()

    Log.d("ChartScreen", "Segments count: ${segments.size}")
    val entriesAsc = entries.sortedBy { it.date }

    ChartLayout(entries = entriesAsc, goal = goal, segments = segments, weightUnit = weightUnit, projection = goalProjection)
}
