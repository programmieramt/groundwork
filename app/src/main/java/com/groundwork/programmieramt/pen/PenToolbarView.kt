package com.groundwork.programmieramt.pen

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space

class PenToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    data class ToolSelection(val color: Int, val strokeWidth: Float, val isMarker: Boolean, val isEraser: Boolean)

    var onToolSelected: (ToolSelection) -> Unit = {}

    private data class Tool(val color: Int, val strokeWidth: Float, val isMarker: Boolean, val isEraser: Boolean = false)

    private val tools = listOf(
        Tool(Color.BLACK, PEN_WIDTH, isMarker = false),
        Tool(Color.parseColor("#C62828"), PEN_WIDTH, isMarker = false),
        Tool(Color.parseColor("#1565C0"), PEN_WIDTH, isMarker = false),
        Tool(Color.parseColor("#FFEB3B"), MARKER_WIDTH, isMarker = true),
        Tool(Color.parseColor("#A5D6A7"), MARKER_WIDTH, isMarker = true),
        Tool(Color.parseColor("#FFCC80"), MARKER_WIDTH, isMarker = true),
        Tool(Color.LTGRAY, 0f, isMarker = false, isEraser = true)
    )

    private val swatches = mutableListOf<FrameLayout>()
    private var selectedIndex = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.parseColor("#EEEEEE"))
        val dp = resources.displayMetrics.density
        val padding = (6 * dp).toInt()
        setPadding(padding, padding, padding, padding)
        val size = (32 * dp).toInt()
        val margin = (8 * dp).toInt()

        tools.forEachIndexed { index, tool ->
            if (index == PEN_COUNT || index == PEN_COUNT + MARKER_COUNT) {
                addView(Space(context).apply {
                    layoutParams = LayoutParams((16 * dp).toInt(), 1)
                })
            }
            val swatch = FrameLayout(context).apply {
                layoutParams = LayoutParams(size, size).apply {
                    marginEnd = margin
                }
                setOnClickListener {
                    selectedIndex = index
                    refreshSwatches()
                    onToolSelected(toolSelection(tool))
                }
            }
            swatches.add(swatch)
            addView(swatch)
        }
        refreshSwatches()
    }

    private fun refreshSwatches() {
        val dp = resources.displayMetrics.density
        tools.forEachIndexed { index, tool ->
            swatches[index].background = GradientDrawable().apply {
                shape = if (tool.isEraser) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
                if (tool.isEraser) cornerRadius = 4 * dp
                setColor(tool.color)
                if (index == selectedIndex) {
                    setStroke((3 * dp).toInt(), Color.parseColor("#1B5E20"))
                } else {
                    setStroke((1 * dp).toInt(), Color.parseColor("#9E9E9E"))
                }
            }
        }
    }

    private fun toolSelection(tool: Tool) =
        ToolSelection(tool.color, tool.strokeWidth, tool.isMarker, tool.isEraser)

    fun currentTool(): ToolSelection = toolSelection(tools[selectedIndex])

    companion object {
        private const val PEN_COUNT = 3
        private const val MARKER_COUNT = 3
        const val PEN_WIDTH = 3.0f
        const val MARKER_WIDTH = 48.0f
    }
}
