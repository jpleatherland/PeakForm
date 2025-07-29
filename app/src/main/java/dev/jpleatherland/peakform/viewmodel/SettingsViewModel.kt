package dev.jpleatherland.peakform.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jpleatherland.peakform.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class WeightUnit {
    KG,
    LB,
}

class SettingsViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val _weightUnit = MutableStateFlow(WeightUnit.KG)
    val weightUnit: StateFlow<WeightUnit> = _weightUnit

    init {
        viewModelScope.launch {
            SettingsDataStore.getUnit(app).collect {
                _weightUnit.value =
                    when (it.lowercase()) {
                        "kg" -> WeightUnit.KG
                        "lb" -> WeightUnit.LB
                        else -> WeightUnit.KG // Default to KG if unknown
                    }
            }
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            SettingsDataStore.setUnit(
                getApplication(),
                when (unit) {
                    WeightUnit.KG -> "kg"
                    WeightUnit.LB -> "lb"
                },
            )
            _weightUnit.value = unit
            Log.d("SettingsViewModel", "Weight unit set to: $unit")
        }
    }
}
