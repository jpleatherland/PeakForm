package dev.jpleatherland.peakform.data

import android.content.Context
import androidx.room.Room

fun provideDatabase(context: Context): AppDatabase =
    Room
        .databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "weight-tracker-db",
        ).fallbackToDestructiveMigration(true)
        .build()
