package com.motivawall.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

object PdfRendererUtil {
    fun pageCount(context: Context, uri: Uri): Int =
        open(context, uri)?.use { it.pageCount } ?: 0

    fun renderPage(context: Context, uri: Uri, pageIndex: Int, rotation: Int = 0): Bitmap? {
        val renderer = open(context, uri) ?: return null
        renderer.use {
            if (pageIndex !in 0 until it.pageCount) return null
            it.openPage(pageIndex).use { page ->
                val scale = 2f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                raw.eraseColor(Color.WHITE)
                page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                if (rotation == 0) return raw
                val matrix = Matrix().apply { postRotate(rotation.toFloat(), raw.width / 2f, raw.height / 2f) }
                return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            }
        }
    }

    private fun open(context: Context, uri: Uri): PdfRenderer? {
        val descriptor: ParcelFileDescriptor =
            context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return PdfRenderer(descriptor)
    }
}