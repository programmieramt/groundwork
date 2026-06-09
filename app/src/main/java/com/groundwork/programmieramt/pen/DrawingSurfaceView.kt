package com.groundwork.programmieramt.pen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.groundwork.programmieramt.da.Stroke
import com.groundwork.programmieramt.da.StrokePoint
import com.onyx.android.sdk.pen.TouchHelper
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * SurfaceView that integrates Boox TouchHelper for native stylus input.
 * Falls back to MotionEvent rendering on non-Boox devices automatically.
 */
class DrawingSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    var onStrokeAdded: (Stroke) -> Unit = {}
    var onStrokeErased: (UUID) -> Unit = {}
    var currentStrokes: () -> List<Stroke> = { emptyList() }

    private var touchHelper: TouchHelper? = null
    private var isBooxDevice = false

    // Fallback MotionEvent drawing state
    private val activeFallbackPoints = mutableListOf<StrokePoint>()
    private var fallbackFirstTimestamp = 0L

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
        strokeWidth = 3f
    }

    init {
        setZOrderMediaOverlay(false)
        holder.setFormat(PixelFormat.OPAQUE)
        keepScreenOn = true
        holder.addCallback(this)
    }

    // --- SurfaceHolder.Callback ---

    override fun surfaceCreated(holder: SurfaceHolder) {
        initTouchHelper()
        redrawAll()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateTouchHelperLimitRect(width, height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        touchHelper?.closeRawDrawing()
    }

    // --- TouchHelper setup ---

    private fun initTouchHelper() {
        val penCallback = BooxPenCallback(
            onStrokeAdded = { stroke ->
                onStrokeAdded(stroke)
                redrawAll()
            },
            onStrokeErased = { id ->
                onStrokeErased(id)
                redrawAll()
            },
            currentStrokes = currentStrokes
        )

        try {
            touchHelper = TouchHelper.create(this, penCallback)
            isBooxDevice = true
            touchHelper?.setStrokeWidth(3.0f)
            touchHelper?.setStrokeColor(Color.BLACK)
            touchHelper?.openRawDrawing()
            Timber.i("Boox TouchHelper initialized on ${android.os.Build.MODEL}")
        } catch (e: Throwable) {
            Timber.w(e, "TouchHelper unavailable, using MotionEvent fallback")
            touchHelper = null
            isBooxDevice = false
        }
    }

    private fun updateTouchHelperLimitRect(width: Int, height: Int) {
        touchHelper?.setLimitRect(
            Rect(0, 0, width, height),
            ArrayList()
        ) ?: return
    }

    // --- MotionEvent fallback for non-Boox devices ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isBooxDevice) return false

        val toolType = event.getToolType(0)
        val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS
        val isFinger = toolType == MotionEvent.TOOL_TYPE_FINGER

        if (!isStylus && !isFinger) return false

        val x = (10 * event.x).roundToInt() / 10.0f
        val y = (10 * event.y).roundToInt() / 10.0f
        val pressure = (10 * event.pressure).roundToInt() / 10.0f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activeFallbackPoints.clear()
                fallbackFirstTimestamp = System.currentTimeMillis()
                activeFallbackPoints.add(StrokePoint(x, y, pressure))
            }
            MotionEvent.ACTION_MOVE -> {
                val last = activeFallbackPoints.lastOrNull()
                if (last == null || distance(x, y, last.x, last.y) > 2.0) {
                    activeFallbackPoints.add(StrokePoint(x, y, pressure))
                    drawFallbackInProgress()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeFallbackPoints.isNotEmpty()) {
                    val stroke = Stroke(
                        strokeId = UUID.randomUUID(),
                        timestamp = fallbackFirstTimestamp,
                        strokePoints = activeFallbackPoints.toList()
                    )
                    onStrokeAdded(stroke)
                    activeFallbackPoints.clear()
                    redrawAll()
                }
            }
        }
        return true
    }

    // --- Drawing ---

    fun redrawAll() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.WHITE)
            for (stroke in currentStrokes()) {
                drawStroke(canvas, stroke)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawFallbackInProgress() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.WHITE)
            for (stroke in currentStrokes()) {
                drawStroke(canvas, stroke)
            }
            drawPointList(canvas, activeFallbackPoints)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        strokePaint.color = stroke.color
        strokePaint.strokeWidth = stroke.strokeWidth
        drawPointList(canvas, stroke.strokePoints)
    }

    private fun drawPointList(canvas: Canvas, points: List<StrokePoint>) {
        if (points.size < 2) return
        for (i in 1 until points.size) {
            canvas.drawLine(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y, strokePaint)
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = abs(x1 - x2).toDouble()
        val dy = abs(y1 - y2).toDouble()
        return sqrt(dx * dx + dy * dy)
    }
}
