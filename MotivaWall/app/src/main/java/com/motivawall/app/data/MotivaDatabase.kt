package com.motivawall.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WallpaperHistory::class, WallpaperSchedule::class],
    version = 2,
    exportSchema = true
)
abstract class MotivaDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN pdfStartPage INTEGER")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN pdfEndPage INTEGER")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN transitionEffect TEXT NOT NULL DEFAULT 'Fade'")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN autoRotate INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}