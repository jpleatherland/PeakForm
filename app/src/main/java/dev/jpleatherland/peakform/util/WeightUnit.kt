package dev.jpleatherland.peakform.util

import com.github.mikephil.charting.formatter.ValueFormatter
import dev.jpleatherland.peakform.viewmodel.WeightUnit
import java.util.Locale

fun Double.kgToLb(): Double = this * 2.20462

fun Double.lbToKg(): Double = this / 2.20462

fun formatWeight(
    weightKg: Double,
    unit: WeightUnit,
): String =
    when (unit) {
        WeightUnit.KG -> String.format(Locale.getDefault(), "%.1f kg", weightKg)
        WeightUnit.LB -> String.format(Locale.getDefault(), "%.1f lb", weightKg.kgToLb())
    }

class WeightUnitValueFormatter(
    private val unit: WeightUnit,
) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String =
        when (unit) {
            WeightUnit.KG -> String.format(Locale.getDefault(), "%.1f kg", value)
            WeightUnit.LB -> String.format(Locale.getDefault(), "%.1f lb", value)
        }
}

fun getWeightUnitLabel(unit: WeightUnit): String =
    when (unit) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }
