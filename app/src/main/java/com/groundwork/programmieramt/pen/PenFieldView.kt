package com.groundwork.programmieramt.pen

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.groundwork.programmieramt.R
import com.groundwork.programmieramt.da.Stroke
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class PenFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val strokes = mutableListOf<Stroke>()
    private val surfaceView: DrawingSurfaceView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_pen_field, this, true)
        surfaceView = findViewById(R.id.pen_surface)

        surfaceView.onStrokeAdded = { stroke -> strokes.add(stroke) }
        surfaceView.onStrokeErased = { id -> strokes.removeAll { it.strokeId == id } }
        surfaceView.currentStrokes = { strokes.toList() }

        findViewById<View>(R.id.btn_clear_pen).setOnClickListener { clearStrokes() }
    }

    fun getStrokesJson(): String {
        if (strokes.isEmpty()) return ""
        return try { adapter.toJson(strokes) ?: "" } catch (_: Exception) { "" }
    }

    fun setStrokesJson(json: String) {
        strokes.clear()
        if (json.isNotBlank() && json.startsWith("[")) {
            try { adapter.fromJson(json)?.let { strokes.addAll(it) } } catch (_: Exception) {}
        }
        surfaceView.post { surfaceView.redrawAll() }
    }

    private fun clearStrokes() {
        strokes.clear()
        surfaceView.redrawAll()
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder().add(UuidAdapter()).build()
        private val strokesType = Types.newParameterizedType(List::class.java, Stroke::class.java)
        private val adapter by lazy { moshi.adapter<List<Stroke>>(strokesType) }
    }
}
