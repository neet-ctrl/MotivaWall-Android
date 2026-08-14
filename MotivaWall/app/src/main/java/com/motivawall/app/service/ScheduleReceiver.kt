package com.motivawall.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.motivawall.app.core.PdfRendererUtil
import com.motivawall.app.core.PdfTransition
import com.motivawall.app.core.WallpaperApplier
import com.motivawall.app.core.WallpaperTarget
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val days = intent.getStringExtra("days").orEmpty()
        val today = SimpleDateFormat("EEE", Locale.US).format(Date()).uppercase(Locale.US)
        val allowed = when (days) {
            "Daily", "" -> true
            "Weekdays" -> today in setOf("MON", "TUE", "WED", "THU", "FRI")
            "Weekends" -> today in setOf("SAT", "SUN")
            else -> days.split(",").map { it.trim().take(3).uppercase(Locale.US) }.contains(today)
        }
        val battery = context.getSystemService(android.os.BatteryManager::class.java)
            ?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        if (!allowed || battery in 1..14) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val path = intent.getStringExtra("path") ?: return@launch
                val isPdf = intent.getBooleanExtra("isPdf", false)
                if (isPdf && intent.getBooleanExtra("autoRotate", false)) {
                    val total = intent.getIntExtra("pdfTotalPages", 1).coerceAtLeast(1)
                    val start = intent.getIntExtra("pdfStartPage", 0).coerceIn(0, total - 1)
                    val end = intent.getIntExtra("pdfEndPage", total - 1).coerceIn(start, total - 1)
                    context.getSharedPreferences("pdf_wallpaper", Context.MODE_PRIVATE).edit()
                        .putString("path", path)
                        .putInt("total", total)
                        .putInt("page", intent.getIntExtra("pdfPageNumber", start + 1).minus(1).coerceIn(start, end))
                        .putInt("start", start)
                        .putInt("end", end)
                        .putInt("rotation", intent.getIntExtra("pdfRotation", 0))
                        .putString("transition", intent.getStringExtra("transitionEffect") ?: PdfTransition.Fade.name)
                        .putBoolean("paused", false)
                        .putBoolean("loop", intent.getBooleanExtra("loopPdf", true))
                        .putLong("interval", intent.getLongExtra("intervalMs", 10_000L))
                        .apply()
                    ContextCompat.startForegroundService(context, Intent(context, PdfWallpaperService::class.java))
                    ContextCompat.startForegroundService(context, Intent(context, PdfLockScreenDialogService::class.java))
                } else {
                    val bitmap = if (isPdf) {
                        val page = intent.getIntExtra("pdfPageNumber", 1).minus(1).coerceAtLeast(0)
                        PdfRendererUtil.renderPage(
                            context,
                            android.net.Uri.parse(path),
                            page,
                            intent.getIntExtra("pdfRotation", 0)
                        )
                    } else {
                        com.motivawall.app.core.ImageProcessor.decode(context, android.net.Uri.parse(path))
                    }
                    bitmap?.let { WallpaperApplier.apply(context, it, WallpaperTarget.BOTH) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}