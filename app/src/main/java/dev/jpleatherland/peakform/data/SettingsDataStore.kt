package dev.jpleatherland.peakform.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SettingsDataStore {
    // Extension property for DataStore instance
    private val Context.dataStore by preferencesDataStore("settings")

    private val UNIT_KEY = stringPreferencesKey("weight_unit")

    // Save the unit ("kg" or "lb")
    suspend fun setUnit(
        context: Context,
        unit: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[UNIT_KEY] = unit
        }
    }

    // Get the unit as a Flow
    fun getUnit(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[UNIT_KEY] ?: "kg" // Default to "kg"
        }
}
