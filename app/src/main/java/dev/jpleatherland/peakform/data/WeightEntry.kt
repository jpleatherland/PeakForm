package dev.jpleatherland.peakform.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["date"], unique = true)],
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val weight: Double? = null,
    val weightSource: String? = null,
    val calories: Int? = null,
    val caloriesSource: String? = null,
)
