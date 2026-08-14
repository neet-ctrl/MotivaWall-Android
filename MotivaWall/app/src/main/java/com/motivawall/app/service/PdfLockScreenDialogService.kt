package com.motivawall.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.motivawall.app.MainActivity
import com.motivawall.app.R

class PdfLockScreenDialogService : Service() {
    companion object {
        const val ACTION_NEXT = "com.motivawall.app.NEXT_PAGE"
        const val ACTION_PREV = "com.motivawall.app.PREV_PAGE"
        const val ACTION_PAUSE = "com.motivawall.app.PAUSE_AUTO"
        const val ACTION_CLOSE = "com.motivawall.app.CLOSE_DIALOG"
        const val ACTION_SHOW = "com.motivawall.app.SHOW_DIALOG"
        const val ACTION_UPDATE_PAGE = "com.motivawall.app.UPDATE_PAGE"
        const val EXTRA_PAGE = "current_page"
        const val EXTRA_TOTAL = "total_pages"
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    private var page = 0
    private var total = 1
    private var paused = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(22, notification())
        val filter = IntentFilter().apply {
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_PAUSE)
            addAction(ACTION_CLOSE)
            addAction(ACTION_SHOW)
            addAction(ACTION_UPDATE_PAGE)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(controlReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(controlReceiver, filter)
        }
        if (Settings.canDrawOverlays(this)) showDialog()
    }

    private fun showDialog() {
        if (floatingView != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            setBackgroundColor(Color.argb(225, 26, 26, 46))
        }
        val title = TextView(this).apply {
            text = "PDF WALLPAPER"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            typeface = Typeface.DEFAULT_BOLD
        }
        val pageText = TextView(this).apply {
            tag = "page"
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            tag = "progress"
            max = 100
        }
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        controls.addView(actionButton("PREV") { changePage(-1) })
        controls.addView(actionButton(if (paused) "PLAY" else "PAUSE") { paused = !paused; updateDialog() })
        controls.addView(actionButton("NEXT") { changePage(1) })
        val close = TextView(this).apply {
            text = "×"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { hideDialog() }
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            addView(close, LinearLayout.LayoutParams(56, 56))
        }
        root.addView(header)
        root.addView(pageText, LinearLayout.LayoutParams(-1, 80))
        root.addView(progress, LinearLayout.LayoutParams(-1, 12))
        root.addView(controls, LinearLayout.LayoutParams(-1, 72))
        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * .86f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (resources.displayMetrics.heightPixels * .62f).toInt()
        }
        var downX = 0f
        var downY = 0f
        root.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x += (event.rawX - downX).toInt()
                    params.y += (event.rawY - downY).toInt()
                    downX = event.rawX; downY = event.rawY
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> true
            }
        }
        floatingView = root
        windowManager.addView(root, params)
        updateDialog()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.WHITE)
        setOnClickListener { vibrate(); action() }
    }

    private fun changePage(delta: Int) {
        page = (page + delta).coerceIn(0, (total - 1).coerceAtLeast(0))
        getSharedPreferences("pdf_wallpaper", MODE_PRIVATE).edit().putInt("page", page).apply()
        sendBroadcast(Intent(ACTION_UPDATE_PAGE).setPackage(packageName).putExtra(EXTRA_PAGE, page).putExtra(EXTRA_TOTAL, total))
        updateDialog()
    }

    private fun updateDialog() {
        val root = floatingView ?: return
        root.findViewWithTag<TextView>("page")?.text = "Page ${page + 1}  /  $total"
        root.findViewWithTag<ProgressBar>("progress")?.progress = ((page + 1) * 100 / total.coerceAtLeast(1))
    }

    private fun hideDialog() {
        floatingView?.let { windowManager.removeView(it) }
        floatingView = null
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(45)
    }

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_NEXT -> changePage(1)
                ACTION_PREV -> changePage(-1)
                ACTION_PAUSE -> { paused = !paused; updateDialog() }
                ACTION_CLOSE -> hideDialog()
                ACTION_SHOW -> showDialog()
                ACTION_UPDATE_PAGE -> {
                    page = intent.getIntExtra(EXTRA_PAGE, page)
                    total = intent.getIntExtra(EXTRA_TOTAL, total)
                    updateDialog()
                }
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("pdf_controls", getString(R.string.channel_pdf), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, "pdf_controls")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("MotivaWall controls")
            .setContentText("Tap to reopen PDF controls")
            .setContentIntent(android.app.PendingIntent.getActivity(
                this, 1, Intent(this, MainActivity::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            ))
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        hideDialog()
        unregisterReceiver(controlReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}