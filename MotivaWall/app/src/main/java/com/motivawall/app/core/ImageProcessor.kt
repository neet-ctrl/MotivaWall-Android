package com.motivawall.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.ColorUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class ImageEdits(
    val ratio: String = "Free",
    val rotation: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val brightness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50,
    val vignette: Int = 0,
    val quote: String = "",
    val author: String = "",
    val textPosition: String = "Center",
    val textColor: String = "#FFFFFF",
    val textSize: String = "Medium"
)

object ImageProcessor {
    fun decode(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)

    fun render(source: Bitmap, edits: ImageEdits): Bitmap {
        val matrix = Matrix().apply {
            postRotate(edits.rotation.toFloat())
            postScale(if (edits.flipX) -1f else 1f, if (edits.flipY) -1f else 1f)
        }
        var result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val targetRatio = ratioValue(edits.ratio)
        if (targetRatio != null) {
            val current = result.width.toFloat() / result.height
            val cropWidth: Int
            val cropHeight: Int
            if (current > targetRatio) {
                cropHeight = result.height
                cropWidth = (cropHeight * targetRatio).toInt()
            } else {
                cropWidth = result.width
                cropHeight = (cropWidth / targetRatio).toInt()
            }
            val x = (result.width - cropWidth) / 2
            val y = (result.height - cropHeight) / 2
            result = Bitmap.createBitmap(result, x, y, cropWidth, cropHeight)
        }

        val output = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val brightness = (edits.brightness - 50) * 2.55f
        val contrast = edits.contrast / 50f
        val saturation = edits.saturation / 50f
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f + brightness
        val saturationMatrix = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
        val adjust = android.graphics.ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        saturationMatrix.postConcat(adjust)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(saturationMatrix)
        canvas.drawBitmap(result, 0f, 0f, paint)

        if (edits.vignette > 0) {
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    output.width / 2f,
                    output.height / 2f,
                    min(output.width, output.height) * .72f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(edits.vignette * 2, 0, 0, 0)),
                    floatArrayOf(.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vignettePaint)
        }

        if (edits.quote.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = try { Color.parseColor(edits.textColor) } catch (_: Exception) { Color.WHITE }
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                textSize = when (edits.textSize) {
                    "Small" -> output.width * .045f
                    "Large" -> output.width * .085f
                    else -> output.width * .062f
                }
                setShadowLayer(14f, 0f, 3f, Color.argb(180, 0, 0, 0))
            }
            val x = output.width / 2f
            val y = when (edits.textPosition) {
                "Top" -> output.height * .18f
                "Bottom" -> output.height * .82f
                else -> output.height * .52f
            }
            val lines = wrap(edits.quote, textPaint, output.width * .82f)
            lines.forEachIndexed { index, line -> canvas.drawText(line, x, y + index * textPaint.textSize * 1.25f, textPaint) }
            if (edits.author.isNotBlank()) {
                textPaint.textSize *= .55f
                canvas.drawText("— ${edits.author}", x, y + lines.size * textPaint.textSize * 1.7f, textPaint)
            }
        }
        return output
    }

    fun save(context: Context, bitmap: Bitmap, name: String = "wallpaper_${System.currentTimeMillis()}"): File {
        val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val file = File(dir, "$name.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        return file
    }

    private fun ratioValue(name: String): Float? = when (name) {
        "16:9" -> 16f / 9f
        "18:9" -> 2f
        "20:9" -> 20f / 9f
        "4:3" -> 4f / 3f
        "1:1" -> 1f
        else -> null
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        text.split(" ").forEach { word ->
            val next = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(next) > maxWidth && line.isNotEmpty()) {
                lines += line
                line = word
            } else line = next
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }
}