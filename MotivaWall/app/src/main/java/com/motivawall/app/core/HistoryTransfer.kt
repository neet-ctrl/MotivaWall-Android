package com.motivawall.app.core

import com.motivawall.app.data.WallpaperHistory
import org.json.JSONArray
import org.json.JSONObject

object HistoryTransfer {
    fun encode(items: List<WallpaperHistory>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("sourcePath", item.sourcePath)
                put("thumbnailPath", item.thumbnailPath)
                put("dateSet", item.dateSet)
                put("isFavorite", item.isFavorite)
                put("isPdf", item.isPdf)
                put("pdfPageNumber", item.pdfPageNumber)
                put("pdfTotalPages", item.pdfTotalPages)
                put("pdfStartPage", item.pdfStartPage)
                put("pdfEndPage", item.pdfEndPage)
                put("pdfRotation", item.pdfRotation)
                put("transitionEffect", item.transitionEffect)
                put("autoRotate", item.autoRotate)
                put("loopPdf", item.loopPdf)
                put("intervalMs", item.intervalMs)
                put("brightness", item.brightness)
                put("contrast", item.contrast)
                put("saturation", item.saturation)
                put("vignette", item.vignette)
                put("textOverlay", item.textOverlay)
                put("textAuthor", item.textAuthor)
                put("textColor", item.textColor)
                put("textSize", item.textSize)
                put("textPosition", item.textPosition)
                put("fontStyle", item.fontStyle)
                put("cropRatio", item.cropRatio)
            })
        }
        return array.toString(2)
    }

    fun decode(json: String): List<WallpaperHistory> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    WallpaperHistory(
                        sourcePath = item.optString("sourcePath"),
                        thumbnailPath = item.optString("thumbnailPath"),
                        dateSet = item.optLong("dateSet"),
                        isFavorite = item.optBoolean("isFavorite"),
                        isPdf = item.optBoolean("isPdf"),
                        pdfPageNumber = item.optIntOrNull("pdfPageNumber"),
                        pdfTotalPages = item.optIntOrNull("pdfTotalPages"),
                        pdfStartPage = item.optIntOrNull("pdfStartPage"),
                        pdfEndPage = item.optIntOrNull("pdfEndPage"),
                        pdfRotation = item.optInt("pdfRotation", 0),
                        transitionEffect = item.optString("transitionEffect", "Fade"),
                        autoRotate = item.optBoolean("autoRotate"),
                        loopPdf = item.optBoolean("loopPdf", true),
                        intervalMs = item.optLong("intervalMs", 10_000L),
                        brightness = item.optInt("brightness", 50),
                        contrast = item.optInt("contrast", 50),
                        saturation = item.optInt("saturation", 50),
                        vignette = item.optInt("vignette"),
                        textOverlay = item.optString("textOverlay"),
                        textAuthor = item.optString("textAuthor"),
                        textColor = item.optString("textColor", "#FFFFFF"),
                        textSize = item.optString("textSize", "Medium"),
                        textPosition = item.optString("textPosition", "Center"),
                        fontStyle = item.optString("fontStyle", "Sans Serif"),
                        cropRatio = item.optString("cropRatio", "Free")
                    )
                )
            }
        }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key) || !has(key)) null else optInt(key)
}