package com.motivawall.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.motivawall.R
import com.motivawall.app.core.PdfTransition
import com.motivawall.app.core.PdfWallpaperController

class PdfWallpaperService : Service() {
    companion object {
        const val ACTION_STOP = "com.motivawall.app.STOP_PDF_ROTATION"
    }

    private val prefs by lazy { getSharedPreferences("pdf_wallpaper", MODE_PRIVATE) }
    private var running = true
    private var worker: Thread? = null
    private var previousPage: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(21, notification())
        startRotation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    private fun startRotation() {
        worker = Thread {
            while (running) {
                val path = prefs.getString("path", null)
                val total = prefs.getInt("total", 0)
                val start = prefs.getInt("start", 0).coerceIn(0, (total - 1).coerceAtLeast(0))
                val end = prefs.getInt("end", (total - 1).coerceAtLeast(0)).coerceIn(start, (total - 1).coerceAtLeast(start))
                val interval = prefs.getLong("interval", 10_000L).coerceAtLeast(3_000L)
                val paused = prefs.getBoolean("paused", false)
                val loop = prefs.getBoolean("loop", true)
                if (path == null || total == 0 || paused) {
                    Thread.sleep(500L)
                    continue
                }

                val page = prefs.getInt("page", start).coerceIn(start, end)
                val effect = runCatching {
                    PdfTransition.valueOf(prefs.getString("transition", PdfTransition.Fade.name) ?: PdfTransition.Fade.name)
                }.getOrDefault(PdfTransition.Fade)
                previousPage = PdfWallpaperController.setPage(
                    this,
                    android.net.Uri.parse(path),
                    page,
                    prefs.getInt("rotation", 0),
                    effect,
                    previousPage,
                    android.app.WallpaperManager.FLAG_LOCK or android.app.WallpaperManager.FLAG_SYSTEM
                )
                sendPageUpdate(page, total, start, end)
                if (!loop && page >= end) {
                    prefs.edit().putBoolean("paused", true).apply()
                    continue
                }
                val nextPage = if (page >= end) start else page + 1
                prefs.edit().putInt("page", nextPage).apply()
                var slept = 0L
                while (running && slept < interval) {
                    Thread.sleep(minOf(500L, interval - slept))
                    slept += 500L
                }
            }
        }.apply { start() }
    }

    private fun sendPageUpdate(page: Int, total: Int, start: Int, end: Int) {
        sendBroadcast(Intent(PdfLockScreenDialogService.ACTION_UPDATE_PAGE).apply {
            setPackage(packageName)
            putExtra(PdfLockScreenDialogService.EXTRA_PAGE, page)
            putExtra(PdfLockScreenDialogService.EXTRA_TOTAL, total)
            putExtra(PdfLockScreenDialogService.EXTRA_START, start)
            putExtra(PdfLockScreenDialogService.EXTRA_END, end)
        })
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        previousPage?.recycle()
        previousPage = null
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