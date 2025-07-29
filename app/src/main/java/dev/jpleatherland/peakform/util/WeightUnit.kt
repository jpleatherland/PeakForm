package dev.jpleatherland.peakform.util

import dev.jpleatherland.peakform.viewmodel.WeightUnit
import java.util.Locale

fun Double.kgToLb(): Double = this * 2.20462

fun Double.lgToKg(): Double = this / 2.20462

fun formatWeight(
    weightKg: Double,
    unit: WeightUnit,
): String =
    when (unit) {
        WeightUnit.KG -> String.format(Locale.getDefault(), "%.1f kg", weightKg)
        WeightUnit.LB -> String.format(Locale.getDefault(), "%.1f lb", weightKg.kgToLb())
    }
