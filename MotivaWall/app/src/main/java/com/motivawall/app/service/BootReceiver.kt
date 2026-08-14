package com.motivawall.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.motivawall.app.data.MotivaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val database = Room.databaseBuilder(context, MotivaDatabase::class.java, "motivawall.db")
                    .addMigrations(MotivaDatabase.MIGRATION_1_2)
                    .build()
                runCatching {
                    database.wallpaperDao().getSchedules().forEach { schedule ->
                        database.wallpaperDao().findById(schedule.wallpaperId)?.let { source ->
                            SchedulePlanner.schedule(context, schedule, source.sourcePath, source.isPdf)
                        }
                    }
                }
                database.close()
                pending.finish()
            }
            val pdfPrefs = context.getSharedPreferences("pdf_wallpaper", Context.MODE_PRIVATE)
            if (
                pdfPrefs.getString("path", null) != null &&
                pdfPrefs.getBoolean("autoRotate", true) &&
                !pdfPrefs.getBoolean("paused", false)
            ) {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, PdfWallpaperService::class.java)
                )
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, PdfLockScreenDialogService::class.java)
                )
            }
        }
    }
}