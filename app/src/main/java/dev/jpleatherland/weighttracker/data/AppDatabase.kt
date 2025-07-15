package dev.jpleatherland.weighttracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WeightEntry::class, Goal::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    abstract fun goalDao(): GoalDao
}
