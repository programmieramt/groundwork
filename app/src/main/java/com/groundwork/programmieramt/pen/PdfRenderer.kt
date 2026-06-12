package com.groundwork.programmieramt.pen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.pdf.PdfDocument
import com.groundwork.programmieramt.da.Stroke
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.ByteArrayOutputStream

object PdfRenderer {

    private const val MARKER_ALPHA = 100
    private const val PAGE_WIDTH = 1404
    private const val PAGE_HEIGHT = 1872

    private val moshi = Moshi.Builder().add(UuidAdapter()).build()
    private val strokesType = Types.newParameterizedType(List::class.java, Stroke::class.java)
    private val strokesAdapter by lazy { moshi.adapter<List<Stroke>>(strokesType) }

    fun render(strokesJson: String, drawTemplate: (Canvas, Int, Int) -> Unit): ByteArray {
        val strokes = if (strokesJson.isNotBlank() && strokesJson.startsWith("[")) {
            try { strokesAdapter.fromJson(strokesJson) ?: emptyList() } catch (_: Exception) { emptyList() }
        } else emptyList()

        val markerXfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val pdf = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            drawTemplate(canvas, PAGE_WIDTH, PAGE_HEIGHT)

            for (stroke in strokes) {
                paint.color = stroke.color
                paint.strokeWidth = stroke.strokeWidth
                paint.alpha = if (stroke.isMarker) MARKER_ALPHA else 255
                paint.xfermode = if (stroke.isMarker) markerXfermode else null
                val points = stroke.strokePoints
                for (i in 1 until points.size) {
                    canvas.drawLine(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y, paint)
                }
                paint.xfermode = null
            }

            pdf.finishPage(page)

            val out = ByteArrayOutputStream()
            pdf.writeTo(out)
            return out.toByteArray()
        } finally {
            pdf.close()
        }
    }
}
