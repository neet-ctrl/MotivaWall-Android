package com.motivawall.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WallpaperHistory::class, WallpaperSchedule::class],
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE schedules ADD COLUMN isPdf INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE schedules ADD COLUMN pdfPageNumber INTEGER")
                database.execSQL("ALTER TABLE schedules ADD COLUMN pdfTotalPages INTEGER")
                database.execSQL("ALTER TABLE schedules ADD COLUMN pdfStartPage INTEGER")
                database.execSQL("ALTER TABLE schedules ADD COLUMN pdfEndPage INTEGER")
                database.execSQL("ALTER TABLE schedules ADD COLUMN pdfRotation INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE schedules ADD COLUMN transitionEffect TEXT NOT NULL DEFAULT 'Fade'")
                database.execSQL("ALTER TABLE schedules ADD COLUMN autoRotate INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE schedules ADD COLUMN loopPdf INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE schedules ADD COLUMN intervalMs INTEGER NOT NULL DEFAULT 10000")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN pdfRotation INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN loopPdf INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE wallpaper_history ADD COLUMN intervalMs INTEGER NOT NULL DEFAULT 10000")
                database.execSQL(
                    "UPDATE schedules SET " +
                        "isPdf = COALESCE((SELECT isPdf FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 0), " +
                        "pdfPageNumber = (SELECT pdfPageNumber FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), " +
                        "pdfTotalPages = (SELECT pdfTotalPages FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), " +
                        "pdfStartPage = (SELECT pdfStartPage FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), " +
                        "pdfEndPage = (SELECT pdfEndPage FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), " +
                        "pdfRotation = COALESCE((SELECT pdfRotation FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 0), " +
                        "transitionEffect = COALESCE((SELECT transitionEffect FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 'Fade'), " +
                        "autoRotate = COALESCE((SELECT autoRotate FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 0), " +
                        "loopPdf = COALESCE((SELECT loopPdf FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 1), " +
                        "intervalMs = COALESCE((SELECT intervalMs FROM wallpaper_history WHERE wallpaper_history.id = schedules.wallpaperId), 10000)"
                )
            }
        }
    }
}