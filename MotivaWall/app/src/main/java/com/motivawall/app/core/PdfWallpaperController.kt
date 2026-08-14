package com.motivawall.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.SystemClock
import kotlin.math.max

enum class PdfTransition { Fade, Slide, Zoom, Flip, Morph }

object PdfWallpaperController {
    fun setPage(
        context: Context,
        uri: Uri,
        page: Int,
        rotation: Int,
        effect: PdfTransition,
        previous: Bitmap?,
        flags: Int
    ): Bitmap? {
        val next = PdfRendererUtil.renderPage(context, uri, page, rotation) ?: return previous
        if (previous == null || previous.width != next.width || previous.height != next.height) {
            WallpaperApplier.applyFlags(context, next, flags)
            return next
        }
        val steps = 6
        for (step in 1..steps) {
            val progress = step.toFloat() / steps
            val frame = frame(previous, next, effect, progress)
            WallpaperApplier.applyFlags(context, frame, flags)
            frame.recycle()
            if (step < steps) SystemClock.sleep(42L)
        }
        return next
    }

    private fun frame(previous: Bitmap, next: Bitmap, effect: PdfTransition, progress: Float): Bitmap {
        val output = Bitmap.createBitmap(next.width, next.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val w = next.width.toFloat()
        val h = next.height.toFloat()
        when (effect) {
            PdfTransition.Fade -> {
                paint.alpha = 255
                canvas.drawBitmap(previous, 0f, 0f, paint)
                paint.alpha = (progress * 255).toInt()
                canvas.drawBitmap(next, 0f, 0f, paint)
            }
            PdfTransition.Slide -> {
                canvas.drawBitmap(previous, -w * progress, 0f, paint)
                canvas.drawBitmap(next, w * (1f - progress), 0f, paint)
            }
            PdfTransition.Zoom -> {
                val oldScale = 1f + progress * .12f
                val newScale = .88f + progress * .12f
                drawCentered(canvas, previous, oldScale, 255)
                drawCentered(canvas, next, newScale, (progress * 255).toInt())
            }
            PdfTransition.Flip -> {
                val oldScale = max(.02f, 1f - progress)
                val newScale = max(.02f, progress)
                drawCentered(canvas, previous, oldScale, if (progress < .5f) 255 else 0)
                drawCentered(canvas, next, newScale, if (progress >= .5f) 255 else 0)
            }
            PdfTransition.Morph -> {
                val oldScale = 1f + progress * .06f
                val newScale = .94f + progress * .06f
                drawCentered(canvas, previous, oldScale, ((1f - progress) * 255).toInt())
                drawCentered(canvas, next, newScale, (progress * 255).toInt())
            }
        }
        return output
    }

    private fun drawCentered(canvas: Canvas, bitmap: Bitmap, scale: Float, alpha: Int) {
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val left = (canvas.width - width) / 2f
        val top = (canvas.height - height) / 2f
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + width, top + height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        )
    }
}