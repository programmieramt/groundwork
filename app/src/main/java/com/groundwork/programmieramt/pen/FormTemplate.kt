package com.groundwork.programmieramt.pen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object FormTemplate {

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BBBBBB")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun drawOneOnOne(canvas: Canvas, w: Int, h: Int) {
        drawEqualSections(canvas, w, h, Color.parseColor("#5B8DD9"), listOf(
            "Thema / Was liegt an?",
            "Das echte Problem",
            "Vereinbarungen",
            "Eindruck & Stimmung",
            "Offene Punkte"
        ))
    }

    fun drawTeamNote(canvas: Canvas, w: Int, h: Int) {
        drawEqualSections(canvas, w, h, Color.parseColor("#E08050"), listOf(
            "Beobachtungen",
            "Stimmung / Dynamik",
            "Spannungen / Offene Punkte",
            "Maßnahmen / Follow-Up"
        ))
    }

    fun drawSofort(canvas: Canvas, w: Int, h: Int) {
        drawSections(canvas, w, h, Color.parseColor("#50A878"),
            labels = listOf("Capture", "Follow-Up"),
            starts = listOf(0f, 0.65f),
            end = 1f
        )
    }

    fun drawTeamMember(canvas: Canvas, w: Int, h: Int) {
        drawEqualSections(canvas, w, h, Color.parseColor("#4CAF50"), listOf(
            "Erster Eindruck",
            "Stärken",
            "Entwicklungsfeld",
            "Motivation",
            "Sensibles"
        ))
    }

    fun drawMeetingNote(canvas: Canvas, w: Int, h: Int) {
        drawEqualSections(canvas, w, h, Color.parseColor("#00897B"), listOf(
            "Agenda",
            "Notizen",
            "Action Items"
        ))
    }

    fun drawFreeNote(canvas: Canvas, w: Int, h: Int) {
        val lineSpacing = (h * 0.048f).coerceIn(36f, 64f)
        var lineY = lineSpacing
        while (lineY < h) {
            canvas.drawLine(0f, lineY, w.toFloat(), lineY, linePaint)
            lineY += lineSpacing
        }
    }

    private fun drawEqualSections(canvas: Canvas, w: Int, h: Int, color: Int, labels: List<String>) {
        val n = labels.size
        drawSections(canvas, w, h, color,
            labels = labels,
            starts = List(n) { i -> i.toFloat() / n },
            end = 1f
        )
    }

    private fun drawSections(
        canvas: Canvas, w: Int, h: Int, color: Int,
        labels: List<String>, starts: List<Float>, end: Float
    ) {
        val headerH = (h * 0.055f).coerceIn(36f, 72f)
        val lineSpacing = (h * 0.048f).coerceIn(36f, 64f)
        labelPaint.textSize = (headerH * 0.56f).coerceIn(18f, 36f)

        for (i in labels.indices) {
            val sectionTop = h * starts[i]
            val sectionBottom = if (i + 1 < starts.size) h * starts[i + 1] else h * end
            val headerBottom = sectionTop + headerH

            // Colored header bar
            headerPaint.color = color
            canvas.drawRect(0f, sectionTop, w.toFloat(), headerBottom, headerPaint)

            // Section label
            canvas.drawText(labels[i], 14f, sectionTop + headerH * 0.72f, labelPaint)

            // Ruled lines in writing area
            var lineY = headerBottom + lineSpacing
            while (lineY < sectionBottom - lineSpacing * 0.4f) {
                canvas.drawLine(0f, lineY, w.toFloat(), lineY, linePaint)
                lineY += lineSpacing
            }

            // Divider between sections
            if (i < labels.size - 1) {
                canvas.drawLine(0f, sectionBottom, w.toFloat(), sectionBottom, dividerPaint)
            }
        }
    }
}
