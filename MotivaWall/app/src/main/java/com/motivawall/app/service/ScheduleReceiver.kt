package com.motivawall.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.motivawall.app.core.PdfRendererUtil
import com.motivawall.app.core.WallpaperApplier
import com.motivawall.app.core.WallpaperTarget
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
                val bitmap = if (isPdf) {
                    PdfRendererUtil.renderPage(context, android.net.Uri.parse(path), 0)
                } else {
                    com.motivawall.app.core.ImageProcessor.decode(context, android.net.Uri.parse(path))
                }
                bitmap?.let { WallpaperApplier.apply(context, it, WallpaperTarget.BOTH) }
            } finally {
                pending.finish()
            }
        }
    }
}