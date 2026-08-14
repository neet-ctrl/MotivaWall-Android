package com.motivawall.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motivawall.app.core.ImageEdits
import com.motivawall.app.core.HistoryTransfer
import com.motivawall.app.core.ImageProcessor
import com.motivawall.app.core.PdfRendererUtil
import com.motivawall.app.core.WallpaperApplier
import com.motivawall.app.core.WallpaperTarget
import com.motivawall.app.data.WallpaperDao
import com.motivawall.app.data.WallpaperHistory
import com.motivawall.app.data.WallpaperSchedule
import com.motivawall.app.service.PdfLockScreenDialogService
import com.motivawall.app.service.PdfWallpaperService
import com.motivawall.app.service.SchedulePlanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupState(
    val source: Uri? = null,
    val isPdf: Boolean = false,
    val originalBitmap: Bitmap? = null,
    val bitmap: Bitmap? = null,
    val pdfPage: Int = 0,
    val pdfPages: Int = 0,
    val pdfStartPage: Int = 0,
    val pdfEndPage: Int = 0,
    val intervalMs: Long = 10_000L,
    val transition: String = "Fade",
    val autoRotate: Boolean = true,
    val loopPdf: Boolean = true,
    val favorite: Boolean = false,
    val edits: ImageEdits = ImageEdits(),
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val message: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val dao: WallpaperDao
) : AndroidViewModel(application) {
    private val _setup = MutableStateFlow(SetupState())
    val setup: StateFlow<SetupState> = _setup.asStateFlow()
    private val _theme = MutableStateFlow(
        application.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", "Dark") ?: "Dark"
    )
    val theme: StateFlow<String> = _theme.asStateFlow()
    private val settingsPrefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _dynamicColor = MutableStateFlow(settingsPrefs.getBoolean("dynamicColor", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()
    private val _animations = MutableStateFlow(settingsPrefs.getBoolean("animations", true))
    val animations: StateFlow<Boolean> = _animations.asStateFlow()
    private val _notifications = MutableStateFlow(settingsPrefs.getBoolean("notifications", true))
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()
    val history = dao.observeHistory()
    val schedules = dao.observeSchedules()

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            val bitmap = ImageProcessor.decode(getApplication(), uri)
            _setup.value = SetupState(source = uri, originalBitmap = bitmap, bitmap = bitmap)
        }
    }

    fun selectPdf(uri: Uri) {
        viewModelScope.launch {
            val pages = PdfRendererUtil.pageCount(getApplication(), uri)
            if (pages !in 1..100) {
                _setup.value = SetupState(message = "Choose a PDF with 1–100 pages.")
                return@launch
            }
            _setup.value = SetupState(
                source = uri,
                isPdf = true,
                pdfPages = pages,
                pdfEndPage = pages - 1,
                bitmap = PdfRendererUtil.renderPage(getApplication(), uri, 0)
            )
        }
    }

    fun updateEdits(edits: ImageEdits) {
        val state = _setup.value
        val bitmap = if (state.isPdf && state.source != null) {
            PdfRendererUtil.renderPage(getApplication(), state.source, state.pdfPage, edits.rotation)
        } else {
            state.originalBitmap?.let { ImageProcessor.render(it, edits) }
        }
        _setup.value = state.copy(edits = edits, bitmap = bitmap)
    }

    fun showPdfPage(page: Int) {
        val state = _setup.value
        val source = state.source ?: return
        val safePage = page.coerceIn(state.pdfStartPage, state.pdfEndPage.coerceAtLeast(state.pdfStartPage))
        _setup.value = state.copy(
            pdfPage = safePage,
            bitmap = PdfRendererUtil.renderPage(getApplication(), source, safePage, state.edits.rotation)
        )
    }

    fun setTarget(target: WallpaperTarget) { _setup.value = _setup.value.copy(target = target) }
    fun setTheme(theme: String) {
        _theme.value = theme
        settingsPrefs.edit().putString("theme", theme).apply()
    }
    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        settingsPrefs.edit().putBoolean("dynamicColor", enabled).apply()
    }
    fun setAnimations(enabled: Boolean) {
        _animations.value = enabled
        settingsPrefs.edit().putBoolean("animations", enabled).apply()
    }
    fun setNotifications(enabled: Boolean) {
        _notifications.value = enabled
        settingsPrefs.edit().putBoolean("notifications", enabled).apply()
    }
    fun setInterval(intervalMs: Long) { _setup.value = _setup.value.copy(intervalMs = intervalMs) }
    fun setTransition(transition: String) { _setup.value = _setup.value.copy(transition = transition) }
    fun setAutoRotate(autoRotate: Boolean) { _setup.value = _setup.value.copy(autoRotate = autoRotate) }
    fun setLoopPdf(loopPdf: Boolean) { _setup.value = _setup.value.copy(loopPdf = loopPdf) }
    fun setFavorite(favorite: Boolean) { _setup.value = _setup.value.copy(favorite = favorite) }
    fun setPageRange(start: Int, end: Int) {
        val state = _setup.value
        val safeStart = start.coerceIn(0, (state.pdfPages - 1).coerceAtLeast(0))
        val safeEnd = end.coerceIn(safeStart, (state.pdfPages - 1).coerceAtLeast(safeStart))
        _setup.value = state.copy(
            pdfStartPage = safeStart,
            pdfEndPage = safeEnd,
            pdfPage = state.pdfPage.coerceIn(safeStart, safeEnd)
        )
        showPdfPage(_setup.value.pdfPage)
    }

    fun applyCurrent() {
        viewModelScope.launch {
            val state = _setup.value
            val bitmap = state.bitmap ?: return@launch
            WallpaperApplier.apply(getApplication(), bitmap, state.target)
            val stored = ImageProcessor.save(getApplication(), bitmap)
            dao.insert(
                WallpaperHistory(
                    sourcePath = state.source?.toString().orEmpty(),
                    thumbnailPath = stored.absolutePath,
                    isPdf = state.isPdf,
                    pdfPageNumber = if (state.isPdf) state.pdfPage + 1 else null,
                    pdfTotalPages = if (state.isPdf) state.pdfPages else null,
                    pdfStartPage = if (state.isPdf) state.pdfStartPage + 1 else null,
                    pdfEndPage = if (state.isPdf) state.pdfEndPage + 1 else null,
                    transitionEffect = state.transition,
                    autoRotate = state.autoRotate,
                    isFavorite = state.favorite,
                    brightness = state.edits.brightness,
                    contrast = state.edits.contrast,
                    saturation = state.edits.saturation,
                    vignette = state.edits.vignette,
                    textOverlay = state.edits.quote,
                    textAuthor = state.edits.author,
                    textColor = state.edits.textColor,
                    textSize = state.edits.textSize,
                    textPosition = state.edits.textPosition,
                    cropRatio = state.edits.ratio
                )
            )
            if (state.isPdf && state.source != null && state.autoRotate) {
                startPdfRotation(state)
            } else if (state.isPdf) {
                stopPdfRotation()
            }
            _setup.value = state.copy(message = "Wallpaper set. Your screen is ready.")
        }
    }

    private fun startPdfRotation(state: SetupState) {
        val uri = state.source ?: return
        val prefs = getApplication<Application>().getSharedPreferences("pdf_wallpaper", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("path", uri.toString())
            .putInt("total", state.pdfPages)
            .putInt("page", state.pdfPage)
            .putInt("start", state.pdfStartPage)
            .putInt("end", state.pdfEndPage)
            .putInt("rotation", state.edits.rotation)
            .putString("transition", state.transition)
            .putBoolean("paused", false)
            .putBoolean("autoRotate", state.autoRotate)
            .putBoolean("loop", state.loopPdf)
            .putLong("interval", state.intervalMs)
            .apply()
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), PdfWallpaperService::class.java))
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), PdfLockScreenDialogService::class.java))
    }

    private fun stopPdfRotation() {
        getApplication<Application>().startService(
            Intent(getApplication(), PdfWallpaperService::class.java).setAction(PdfWallpaperService.ACTION_STOP)
        )
        getApplication<Application>().startService(
            Intent(getApplication(), PdfLockScreenDialogService::class.java).setAction(PdfLockScreenDialogService.ACTION_CLOSE)
        )
    }

    fun toggleFavorite(item: WallpaperHistory) = viewModelScope.launch { dao.update(item.copy(isFavorite = !item.isFavorite)) }
    fun delete(item: WallpaperHistory) = viewModelScope.launch { dao.delete(item) }
    fun clearHistory() = viewModelScope.launch { dao.clear() }
    fun saveSchedule(time: String, days: String, wallpaperId: Long, label: String) = viewModelScope.launch {
        val source = dao.findById(wallpaperId) ?: return@launch
        val id = dao.insertSchedule(WallpaperSchedule(time = time, days = days, wallpaperId = wallpaperId, label = label))
        SchedulePlanner.schedule(getApplication(), WallpaperSchedule(id, time, days, wallpaperId, true, label), source.sourcePath, source.isPdf)
    }
    fun deleteSchedule(schedule: WallpaperSchedule) = viewModelScope.launch {
        SchedulePlanner.cancel(getApplication(), schedule)
        dao.deleteSchedule(schedule)
    }
    fun toggleSchedule(schedule: WallpaperSchedule) = viewModelScope.launch {
        val next = schedule.copy(isActive = !schedule.isActive)
        if (next.isActive) {
            dao.findById(schedule.wallpaperId)?.let { source ->
                SchedulePlanner.schedule(getApplication(), next, source.sourcePath, source.isPdf)
            }
        } else {
            SchedulePlanner.cancel(getApplication(), schedule)
        }
        dao.updateSchedule(next)
    }
    fun clearCache() = viewModelScope.launch {
        getApplication<Application>().cacheDir.deleteRecursively()
        getApplication<Application>().cacheDir.mkdirs()
    }

    fun importHistory(json: String) = viewModelScope.launch {
        HistoryTransfer.decode(json).take(50).forEach { dao.insert(it.copy(id = 0)) }
    }
}