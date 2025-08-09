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
import dev.jpleatherland.peakform.data.GoalType
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

    AndroidView(
        modifier = modifier,
        factory = { context ->
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
        },
        update = { chart ->

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
                        Entry(x, y).apply { data = "type:weight|$dateString" }
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

            // --- ACTUAL TREND LINE (linear regression) ---
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

            // --- PROJECTION / MAINTAIN HANDLING ---
            // Start X = latest logged day; Start Y = latest logged weight (in KG for calc, then converted for display)
            val lastLoggedEntry = entries.filter { it.weight != null }.maxByOrNull { it.date }
            val lastLoggedDateMillis = lastLoggedEntry?.date
            val startWeightKg = lastLoggedEntry?.weight

            // 1) If MAINTAIN: use LimitLine at steady weight; NO projection dataset
            chart.axisLeft.removeAllLimitLines()
            var projectionDataSet: LineDataSet? = null

            if (goal?.type == GoalType.MAINTAIN) {
                val yKg = projection?.finalWeight ?: startWeightKg
                if (yKg != null) {
                    val yDisplay =
                        if (weightUnit == WeightUnit.KG) yKg.toFloat() else yKg.kgToLb().toFloat()
                    val ll =
                        com.github.mikephil.charting.components.LimitLine(yDisplay, "Maintain").apply {
                            enableDashedLine(10f, 10f, 0f)
                            lineWidth = 1.5f
                            textSize = 10f
                        }
                    chart.axisLeft.addLimitLine(ll)
                }
            } else {
                // 2) Non-maintain: draw signed projection line until goalDate (if present)
                if (projection != null && lastLoggedDateMillis != null && startWeightKg != null) {
                    val signedProjEntriesKg =
                        buildSignedProjectionEntries(
                            lastLoggedDateMillis = lastLoggedDateMillis,
                            startWeightKg = startWeightKg,
                            projection = projection,
                        )

                    val signedProjEntriesDisplay =
                        signedProjEntriesKg.map { e ->
                            val yDisplay =
                                if (weightUnit == WeightUnit.KG) {
                                    e.y
                                } else {
                                    e.y * 2.20462f
                                }
                            Entry(e.x, yDisplay)
                        }

                    if (signedProjEntriesDisplay.size >= 2) {
                        projectionDataSet =
                            LineDataSet(
                                signedProjEntriesDisplay,
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
                    }
                }
            }

            // --- Assemble datasets ---
            val dataSets = mutableListOf<ILineDataSet>(lineDataSet, trendDataSet)
            projectionDataSet?.let { dataSets += it }
            chart.data = LineData(dataSets)
            chart.invalidate()
        },
    )
}

/**
 * Build a signed projection from the last logged point to the goal date.
 * - Uses projection.rateKgPerWeek **with sign** (negative for cuts).
 * - X axis uses "days since epoch" (to match your charts).
 * - Returns KG-space entries (caller converts to display units).
 */
fun buildSignedProjectionEntries(
    lastLoggedDateMillis: Long,
    startWeightKg: Double,
    projection: GoalProjection,
): List<Entry> {
    val endDateMillis = projection.goalDate?.time ?: return emptyList()
    if (endDateMillis <= lastLoggedDateMillis) return emptyList()

    val stepDays = 1L
    val startX = TimeUnit.MILLISECONDS.toDays(lastLoggedDateMillis).toFloat()
    val endX = TimeUnit.MILLISECONDS.toDays(endDateMillis).toFloat()

    val ratePerWeekKg = projection.rateKgPerWeek // IMPORTANT: keep SIGN
    val points = ArrayList<Entry>()

    var tMillis = lastLoggedDateMillis
    var idx = 0
    while (tMillis <= endDateMillis) {
        val days = TimeUnit.MILLISECONDS.toDays(tMillis - lastLoggedDateMillis).toDouble()
        val yKg = startWeightKg + (ratePerWeekKg * (days / 7.0))
        val x = TimeUnit.MILLISECONDS.toDays(tMillis).toFloat()
        points += Entry(x, yKg.toFloat())
        idx++
        tMillis = lastLoggedDateMillis + TimeUnit.DAYS.toMillis(stepDays * idx)
    }

    // ensure we hit the exact end X (floating date pickers can drift a day)
    if (points.isNotEmpty() && points.last().x < endX) {
        val totalDays = TimeUnit.MILLISECONDS.toDays(endDateMillis - lastLoggedDateMillis).toDouble()
        val yKgEnd = startWeightKg + (ratePerWeekKg * (totalDays / 7.0))
        points += Entry(endX, yKgEnd.toFloat())
    }

    return points
}

// Helper for linear regression
private fun calculateTrendLine(entries: List<Entry>): List<Entry> {
    if (entries.size < 2) return entries // Not enough for regression

    val n = entries.size
    val sumX = entries.sumOf { it.x.toDouble() }
    val sumY = entries.sumOf { it.y.toDouble() }
    val sumXY = entries.sumOf { (it.x * it.y).toDouble() }
    val sumX2 = entries.sumOf { (it.x * it.x).toDouble() }

    val denom = (n * sumX2 - sumX * sumX)
    if (denom == 0.0) return entries

    val b = (n * sumXY - sumX * sumY) / denom
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
                    Entry(x, y).apply { data = "type:calories|$dateString" }
                }

        // 2. Trend line
        val trendPoints = calculateTrendLine(dataPoints)

        // Clean any previous limit lines
        chart.axisLeft.removeAllLimitLines()

        // 3. Stepped goal line (green, dashed)
        val goalCalories = projection?.targetCalories
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

        // ---- MAINTAIN: always add a horizontal LimitLine if we know the target
        if (goal?.type == GoalType.MAINTAIN && goalCalories != null) {
            val ll =
                com.github.mikephil.charting.components.LimitLine(goalCalories.toFloat(), "Maintain").apply {
                    enableDashedLine(10f, 10f, 0f)
                    lineWidth = 1.5f
                    textSize = 10f
                }
            chart.axisLeft.addLimitLine(ll)
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

    fun toChartX(millis: Long) = TimeUnit.MILLISECONDS.toDays(millis).toFloat()

    val segs = segments.sortedBy { it.startDate }
    val firstEntryMillis = entries.minByOrNull { it.date }?.date
    val lastEntryMillis = entries.maxByOrNull { it.date }?.date
    if (lastEntryMillis == null) return emptyList()

    // We’ll draw across the visible X-range of your chart (from first to last entry),
    // falling back to goal.createdAt as a start bound if earlier.
    val startMillis =
        minOf(
            firstEntryMillis ?: lastEntryMillis,
            goal?.createdAt ?: lastEntryMillis,
        )
    val endMillis = lastEntryMillis

    val out = mutableListOf<Entry>()

    // No segments: fallback flat line between start and end
    if (segs.isEmpty()) {
        val y = (fallbackGoalCalories ?: return emptyList()).toFloat()
        out += Entry(toChartX(startMillis), y)
        out += Entry(toChartX(endMillis), y)
        return out
    }

    // With segments: draw a stepped line that covers the chart’s visible range
    // 1) Pre-first segment flat (if the chart starts earlier)
    val firstSeg = segs.first()
    if (startMillis < firstSeg.startDate) {
        val y = (fallbackGoalCalories ?: firstSeg.targetCalories).toFloat()
        out += Entry(toChartX(startMillis), y)
        out += Entry(toChartX(firstSeg.startDate), y)
    }

    // 2) Each segment level from its start to either next segment start or chart end
    for ((i, seg) in segs.withIndex()) {
        val segStart = maxOf(seg.startDate, startMillis)
        val segEnd =
            if (i < segs.lastIndex) {
                minOf(segs[i + 1].startDate, endMillis)
            } else {
                minOf(seg.endDate, endMillis)
            }
        if (segStart < segEnd) {
            val y = seg.targetCalories.toFloat()
            out += Entry(toChartX(segStart), y)
            out += Entry(toChartX(segEnd), y)
        }
    }

    // 3) Post-last segment flat (if the chart extends beyond)
    val lastSeg = segs.last()
    if (endMillis > lastSeg.endDate) {
        val y = (fallbackGoalCalories ?: lastSeg.targetCalories).toFloat()
        out += Entry(toChartX(lastSeg.endDate), y)
        out += Entry(toChartX(endMillis), y)
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

    val entriesAsc = entries.sortedBy { it.date }
    ChartLayout(
        entries = entriesAsc,
        goal = goal,
        segments = segments,
        weightUnit = weightUnit,
        projection = goalProjection,
    )
}
