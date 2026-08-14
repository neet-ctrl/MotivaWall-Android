package com.motivawall.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_history")
data class WallpaperHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePath: String,
    val thumbnailPath: String,
    val dateSet: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPdf: Boolean = false,
    val pdfPageNumber: Int? = null,
    val pdfTotalPages: Int? = null,
    val pdfStartPage: Int? = null,
    val pdfEndPage: Int? = null,
    val transitionEffect: String = "Fade",
    val autoRotate: Boolean = false,
    val brightness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50,
    val vignette: Int = 0,
    val textOverlay: String = "",
    val textAuthor: String = "",
    val textColor: String = "#FFFFFF",
    val textSize: String = "Medium",
    val textPosition: String = "Center",
    val fontStyle: String = "Sans Serif",
    val cropRatio: String = "Free"
)

@Entity(tableName = "schedules")
data class WallpaperSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: String,
    val days: String,
    val wallpaperId: Long,
    val isActive: Boolean = true,
    val label: String = "Wallpaper moment"
)