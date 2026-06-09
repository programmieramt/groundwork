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
import android.view.ViewTreeObserver
import com.groundwork.programmieramt.da.Stroke
import com.groundwork.programmieramt.da.StrokePoint
import com.onyx.android.sdk.pen.TouchHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DrawingSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    /** Called in redrawAll() after white fill, before strokes. Set to draw the form template. */
    var drawTemplate: (Canvas, Int, Int) -> Unit = { _, _, _ -> }

    private val strokes = mutableListOf<Stroke>()
    private var touchHelper: TouchHelper? = null
    private var isBooxDevice = false

    private val activeFallbackPoints = mutableListOf<StrokePoint>()
    private var fallbackFirstTimestamp = 0L

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
        strokeWidth = 3f
    }

    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        updateLimitRect()
    }

    init {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
        keepScreenOn = true
        holder.addCallback(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        super.onDetachedFromWindow()
    }

    // --- SurfaceHolder.Callback ---

    override fun surfaceCreated(holder: SurfaceHolder) {
        initTouchHelper()
        redrawAll()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateLimitRect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        touchHelper?.closeRawDrawing()
    }

    // --- TouchHelper ---

    private fun initTouchHelper() {
        val penCallback = BooxPenCallback(
            onStrokeAdded = { stroke ->
                strokes.add(stroke)
                redrawAll()
            },
            onStrokeErased = { id ->
                strokes.removeAll { it.strokeId == id }
                redrawAll()
            },
            currentStrokes = { strokes.toList() }
        )
        try {
            touchHelper = TouchHelper.create(this, penCallback)
            isBooxDevice = true
            touchHelper?.setStrokeWidth(3.0f)
            touchHelper?.setStrokeColor(Color.BLACK)
            updateLimitRect()
            touchHelper?.openRawDrawing()
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
            Timber.i("Boox TouchHelper initialized on ${android.os.Build.MODEL}")
        } catch (e: Throwable) {
            Timber.w(e, "TouchHelper unavailable, using MotionEvent fallback")
            touchHelper = null
            isBooxDevice = false
        }
    }

    private fun updateLimitRect() {
        val th = touchHelper ?: return
        val location = IntArray(2)
        getLocationOnScreen(location)
        th.setLimitRect(
            Rect(location[0], location[1], location[0] + width, location[1] + height),
            ArrayList()
        )
    }

    // --- JSON serialisation ---

    fun getStrokesJson(): String {
        if (strokes.isEmpty()) return ""
        return try { strokesAdapter.toJson(strokes) ?: "" } catch (_: Exception) { "" }
    }

    fun setStrokesJson(json: String) {
        strokes.clear()
        if (json.isNotBlank() && json.startsWith("[")) {
            try { strokesAdapter.fromJson(json)?.let { strokes.addAll(it) } } catch (_: Exception) {}
        }
        post { redrawAll() }
    }

    // --- Touch events ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        if (isBooxDevice) return true

        val toolType = event.getToolType(0)
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS && toolType != MotionEvent.TOOL_TYPE_FINGER) return true

        val x = (10 * event.x).roundToInt() / 10.0f
        val y = (10 * event.y).roundToInt() / 10.0f
        val pressure = (10 * event.pressure).roundToInt() / 10.0f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
                    strokes.add(stroke)
                    activeFallbackPoints.clear()
                    redrawAll()
                }
            }
        }
        return true
    }

    // --- Drawing ---

    fun redrawAll() {
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        val canvas = holder.lockCanvas() ?: run {
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
            return
        }
        try {
            canvas.drawColor(Color.WHITE)
            drawTemplate(canvas, width, height)
            for (stroke in strokes) drawStroke(canvas, stroke)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
        touchHelper?.setRawDrawingEnabled(true)
        touchHelper?.isRawDrawingRenderEnabled = true
    }

    private fun drawFallbackInProgress() {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.WHITE)
            drawTemplate(canvas, width, height)
            for (stroke in strokes) drawStroke(canvas, stroke)
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

    companion object {
        private val moshi: Moshi = Moshi.Builder().add(UuidAdapter()).build()
        private val strokesType = Types.newParameterizedType(List::class.java, Stroke::class.java)
        private val strokesAdapter by lazy { moshi.adapter<List<Stroke>>(strokesType) }
    }
}
