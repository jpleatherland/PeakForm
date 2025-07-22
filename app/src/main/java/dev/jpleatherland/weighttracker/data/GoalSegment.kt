package dev.jpleatherland.weighttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_segment",
    indices = [Index(value = ["createdAt"], unique = true), Index(value = ["goalId"])],
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GoalSegment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int, // Foreign key to the Goal table
    val startWeight: Double,
    val endWeight: Double,
    val startDate: Long,
    val endDate: Long,
    val source: SegmentSource = SegmentSource.APP,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val targetCalories: Int,
    val ratePerWeek: Double,
)

enum class SegmentSource {
    APP, // Created by the app
    USER, // Created by the user
}
