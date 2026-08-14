package com.motivawall.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import com.motivawall.app.core.PdfTransition
import com.motivawall.app.core.PdfWallpaperController

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
        const val EXTRA_START = "start_page"
        const val EXTRA_END = "end_page"
    }

    private val prefs by lazy { getSharedPreferences("pdf_wallpaper", MODE_PRIVATE) }
    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    private var page = 0
    private var total = 1
    private var startPage = 0
    private var endPage = 0
    private var paused = false
    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(22, notification())
        registerControlReceiver()
        page = prefs.getInt("page", 0)
        total = prefs.getInt("total", 1)
        startPage = prefs.getInt("start", 0)
        endPage = prefs.getInt("end", (total - 1).coerceAtLeast(0))
        paused = prefs.getBoolean("paused", false)
        if (Settings.canDrawOverlays(this)) showDialog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showDialog()
            ACTION_NEXT -> changePage(1)
            ACTION_PREV -> changePage(-1)
            ACTION_PAUSE -> togglePause()
            ACTION_CLOSE -> hideDialog()
            ACTION_UPDATE_PAGE -> {
                page = intent.getIntExtra(EXTRA_PAGE, page)
                total = intent.getIntExtra(EXTRA_TOTAL, total)
                startPage = intent.getIntExtra(EXTRA_START, startPage)
                endPage = intent.getIntExtra(EXTRA_END, endPage)
                updateDialog()
            }
        }
        return START_STICKY
    }

    private fun registerControlReceiver() {
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
        receiverRegistered = true
    }

    private fun showDialog() {
        if (floatingView != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 18)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(238, 30, 28, 64), Color.argb(232, 63, 61, 158))
            ).apply {
                cornerRadius = 34f
                setStroke(1, Color.argb(100, 255, 255, 255))
            }
            setOnLongClickListener {
                startActivity(Intent(this@PdfLockScreenDialogService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        }
        val handle = TextView(this).apply {
            text = "━━━"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(Color.argb(150, 255, 255, 255))
        }
        val title = TextView(this).apply {
            text = "PDF WALLPAPER"
            textSize = 12f
            setTextColor(Color.argb(190, 255, 255, 255))
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
            progressDrawable?.setTint(Color.WHITE)
        }
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(actionButton("Previous") { changePage(-1) }, weightedParams())
            addView(actionButton(if (paused) "Play" else "Pause", "pause") { togglePause() }, weightedParams())
            addView(actionButton("Next") { changePage(1) }, weightedParams())
        }
        val close = TextView(this).apply {
            text = "×"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { vibrate(); hideDialog() }
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            addView(close, LinearLayout.LayoutParams(52, 52))
        }
        root.addView(handle, LinearLayout.LayoutParams(-1, 24))
        root.addView(header)
        root.addView(pageText, LinearLayout.LayoutParams(-1, 76))
        root.addView(progress, LinearLayout.LayoutParams(-1, 10))
        root.addView(controls, LinearLayout.LayoutParams(-1, 70))
        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * .88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (resources.displayMetrics.heightPixels * .58f).toInt()
        }
        var downX = 0f
        var downY = 0f
        root.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x += (event.rawX - downX).toInt()
                    params.y += (event.rawY - downY).toInt()
                    downX = event.rawX
                    downY = event.rawY
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
        floatingView = root
        windowManager.addView(root, params)
        updateDialog()
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, 62, 1f).apply { setMargins(4, 0, 4, 0) }

    private fun actionButton(label: String, tagName: String? = null, action: () -> Unit) = Button(this).apply {
        text = label
        tag = tagName
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(Color.argb(70, 255, 255, 255))
            cornerRadius = 26f
            setStroke(1, Color.argb(75, 255, 255, 255))
        }
        setOnClickListener { vibrate(); action() }
    }

    private fun changePage(delta: Int) {
        val path = prefs.getString("path", null)
        val newPage = (page + delta).let {
            when {
                it > endPage -> startPage
                it < startPage -> endPage
                else -> it
            }
        }
        page = newPage
        prefs.edit().putInt("page", page).apply()
        if (path != null) {
            PdfWallpaperController.setPage(
                this,
                android.net.Uri.parse(path),
                page,
                prefs.getInt("rotation", 0),
                runCatching { PdfTransition.valueOf(prefs.getString("transition", "Fade") ?: "Fade") }.getOrDefault(PdfTransition.Fade),
                null,
                android.app.WallpaperManager.FLAG_LOCK or android.app.WallpaperManager.FLAG_SYSTEM
            )
        }
        updateDialog()
    }

    private fun togglePause() {
        paused = !paused
        prefs.edit().putBoolean("paused", paused).apply()
        updateDialog()
    }

    private fun updateDialog() {
        val root = floatingView ?: return
        root.findViewWithTag<TextView>("page")?.text = "Page ${page + 1}  /  $total"
        root.findViewWithTag<ProgressBar>("progress")?.progress =
            ((page - startPage + 1) * 100 / (endPage - startPage + 1).coerceAtLeast(1))
        root.findViewWithTag<Button>("pause")?.text = if (paused) "Play" else "Pause"
    }

    private fun hideDialog() {
        floatingView?.let {
            runCatching { windowManager.removeView(it) }
        }
        floatingView = null
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(45)
    }

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onStartCommand(intent, 0, 0)
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
            .setContentIntent(
                PendingIntent.getService(
                    this,
                    2,
                    Intent(this, PdfLockScreenDialogService::class.java).setAction(ACTION_SHOW),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        hideDialog()
        if (receiverRegistered) unregisterReceiver(controlReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}