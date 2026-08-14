package com.motivawall.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.motivawall.app.data.WallpaperSchedule
import java.util.Calendar

object SchedulePlanner {
    fun schedule(context: Context, schedule: WallpaperSchedule, sourcePath: String, isPdf: Boolean) {
        val parts = schedule.time.split(":")
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 8)
            set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            putExtra("scheduleId", schedule.id)
            putExtra("days", schedule.days)
            putExtra("path", sourcePath)
            putExtra("isPdf", isPdf)
            putExtra("pdfPageNumber", schedule.pdfPageNumber ?: 1)
            putExtra("pdfTotalPages", schedule.pdfTotalPages ?: 1)
            putExtra("pdfStartPage", schedule.pdfStartPage ?: 0)
            putExtra("pdfEndPage", schedule.pdfEndPage ?: ((schedule.pdfTotalPages ?: 1) - 1))
            putExtra("pdfRotation", schedule.pdfRotation)
            putExtra("transitionEffect", schedule.transitionEffect)
            putExtra("autoRotate", schedule.autoRotate)
            putExtra("loopPdf", schedule.loopPdf)
            putExtra("intervalMs", schedule.intervalMs)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    fun cancel(context: Context, schedule: WallpaperSchedule) {
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)?.cancel(pending)
    }
}