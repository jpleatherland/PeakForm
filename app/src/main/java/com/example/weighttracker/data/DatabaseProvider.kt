package com.example.weighttracker.data

import android.content.Context
import androidx.room.Room

fun provideDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "weight-tracker-db"
    ).build()
}
