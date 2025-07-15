package dev.jpleatherland.weighttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val weight: Double? = null,
    val calories: Int? = null,
)
