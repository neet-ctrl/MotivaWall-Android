package com.motivawall.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.motivawall.app.R
import com.motivawall.app.core.PdfRendererUtil
import java.io.File

class PdfWallpaperService : Service() {
    private val prefs by lazy { getSharedPreferences("pdf_wallpaper", MODE_PRIVATE) }
    private var running = true

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(21, notification())
        Thread {
            while (running) {
                val path = prefs.getString("path", null)
                val page = prefs.getInt("page", 0)
                val total = prefs.getInt("total", 0)
                val interval = prefs.getLong("interval", 10_000L)
                if (path != null && total > 0) {
                    val uri = android.net.Uri.parse(path)
                    PdfRendererUtil.renderPage(this, uri, page)?.let { bitmap ->
                        WallpaperManager.getInstance(this).setBitmap(
                            bitmap, null, true,
                            WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                        )
                        bitmap.recycle()
                    }
                    val next = if (page + 1 >= total) 0 else page + 1
                    prefs.edit().putInt("page", next).apply()
                    sendBroadcast(Intent(PdfLockScreenDialogService.ACTION_UPDATE_PAGE).apply {
                        setPackage(packageName)
                        putExtra(PdfLockScreenDialogService.EXTRA_PAGE, next)
                        putExtra(PdfLockScreenDialogService.EXTRA_TOTAL, total)
                    })
                }
                Thread.sleep(interval.coerceAtLeast(3_000L))
            }
        }.start()
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("pdf", getString(R.string.channel_pdf), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, "pdf")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("MotivaWall PDF wallpaper")
            .setContentText("Your local PDF is rotating on the wallpaper")
            .setOngoing(true)
            .build()
}