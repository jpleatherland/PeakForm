package dev.jpleatherland.weighttracker.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class WeightUnit {
    KG,
    LB,
}

class SettingsViewModel : ViewModel() {
    private val _weightUnit = MutableStateFlow(WeightUnit.KG)
    val weightUnit: StateFlow<WeightUnit> = _weightUnit

    fun setWeightUnit(unit: WeightUnit) {
        _weightUnit.value = unit
    }

    fun toggleWeightUnit() {
        _weightUnit.value =
            if (_weightUnit.value == WeightUnit.KG) WeightUnit.LB else WeightUnit.KG
    }
}
