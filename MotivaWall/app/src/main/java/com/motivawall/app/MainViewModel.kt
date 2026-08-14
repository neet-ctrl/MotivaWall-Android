package com.motivawall.app

import android.app.Application
import android.app.WallpaperManager
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
import java.io.File
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupState(
    val source: Uri? = null,
    val isPdf: Boolean = false,
    val bitmap: Bitmap? = null,
    val pdfPage: Int = 0,
    val pdfPages: Int = 0,
    val intervalMs: Long = 10_000L,
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
    val history = dao.observeHistory()
    val schedules = dao.observeSchedules()

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            val bitmap = ImageProcessor.decode(getApplication(), uri)
            _setup.value = SetupState(source = uri, bitmap = bitmap)
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
                bitmap = PdfRendererUtil.renderPage(getApplication(), uri, 0)
            )
        }
    }

    fun updateEdits(edits: ImageEdits) {
        val state = _setup.value
        val bitmap = if (state.isPdf && state.source != null) {
            PdfRendererUtil.renderPage(getApplication(), state.source, state.pdfPage, edits.rotation)
        } else {
            state.bitmap?.let { ImageProcessor.render(it, edits) }
        }
        _setup.value = state.copy(edits = edits, bitmap = bitmap)
    }

    fun showPdfPage(page: Int) {
        val state = _setup.value
        val source = state.source ?: return
        _setup.value = state.copy(
            pdfPage = page.coerceIn(0, (state.pdfPages - 1).coerceAtLeast(0)),
            bitmap = PdfRendererUtil.renderPage(getApplication(), source, page, state.edits.rotation)
        )
    }

    fun setTarget(target: WallpaperTarget) { _setup.value = _setup.value.copy(target = target) }
    fun setInterval(intervalMs: Long) { _setup.value = _setup.value.copy(intervalMs = intervalMs) }

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
            if (state.isPdf && state.source != null) startPdfRotation(state.source, state.pdfPages)
            _setup.value = state.copy(message = "Wallpaper set. Your screen is ready.")
        }
    }

    private fun startPdfRotation(uri: Uri, pages: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("pdf_wallpaper", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("path", uri.toString())
            .putInt("total", pages)
            .putInt("page", _setup.value.pdfPage)
            .putLong("interval", _setup.value.intervalMs)
            .apply()
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), PdfWallpaperService::class.java))
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), PdfLockScreenDialogService::class.java))
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

    fun importHistory(json: String) = viewModelScope.launch {
        HistoryTransfer.decode(json).take(50).forEach { dao.insert(it.copy(id = 0)) }
    }
}