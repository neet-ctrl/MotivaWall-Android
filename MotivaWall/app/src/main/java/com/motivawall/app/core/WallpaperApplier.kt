package com.motivawall.app.core

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream

object WallpaperApplier {
    fun apply(context: Context, bitmap: Bitmap, target: WallpaperTarget) {
        val manager = WallpaperManager.getInstance(context)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it) }.toByteArray()
        val flags = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        manager.setStream(bytes.inputStream(), null, true, flags)
    }
}

enum class WallpaperTarget { HOME, LOCK, BOTH }