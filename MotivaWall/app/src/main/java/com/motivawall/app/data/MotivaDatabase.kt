package com.motivawall.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WallpaperHistory::class, WallpaperSchedule::class],
    version = 1,
    exportSchema = true
)
abstract class MotivaDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
}