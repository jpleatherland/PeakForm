package dev.jpleatherland.weighttracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WeightEntry::class, Goal::class, GoalSegment::class], version = 9)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    abstract fun goalDao(): GoalDao

    abstract fun goalSegmentDao(): GoalSegmentDao
}
