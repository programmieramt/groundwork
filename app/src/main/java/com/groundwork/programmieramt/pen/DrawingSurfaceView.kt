package com.groundwork.programmieramt.pen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

    var drawTemplate: (Canvas, Int, Int) -> Unit = { _, _, _ -> }

    private val strokes = mutableListOf<Stroke>()
    private var touchHelper: TouchHelper? = null
    private var isBooxDevice = false

    private val activeFallbackPoints = mutableListOf<StrokePoint>()
    private var fallbackFirstTimestamp = 0L

    var currentColor: Int = Color.BLACK
        private set
    var currentStrokeWidth: Float = 3.0f
        private set
    var currentIsMarker: Boolean = false
        private set

    private val markerXfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

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
        Timber.d("init: view created, model=${android.os.Build.MODEL}")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        Timber.d("onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        super.onDetachedFromWindow()
        Timber.d("onDetachedFromWindow")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("surfaceCreated: size=${width}x${height}")
        initTouchHelper()
        redrawAll()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surfaceChanged: ${width}x${height} format=$format")
        if (touchHelper != null) {
            touchHelper?.setRawDrawingEnabled(false)
            updateLimitRect()
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
            Timber.d("surfaceChanged: raw drawing re-enabled after limitRect update")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surfaceDestroyed")
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        touchHelper?.closeRawDrawing()
    }

    private fun initTouchHelper() {
        Timber.d("initTouchHelper: attempting TouchHelper.create")
        val penCallback = BooxPenCallback(
            onStrokeAdded = { stroke ->
                Timber.d("onStrokeAdded: ${stroke.strokePoints.size} points")
                strokes.add(stroke)
                redrawAll()
            },
            onStrokeErased = { id ->
                Timber.d("onStrokeErased: $id")
                strokes.removeAll { it.strokeId == id }
                redrawAll()
            },
            currentStrokes = { strokes.toList() },
            currentColor = { currentColor },
            currentStrokeWidth = { currentStrokeWidth },
            currentIsMarker = { currentIsMarker }
        )
        try {
            touchHelper = TouchHelper.create(this, penCallback)
            isBooxDevice = true
            Timber.d("TouchHelper.create SUCCESS on ${android.os.Build.MODEL}")
            touchHelper?.setStrokeWidth(currentStrokeWidth)
            touchHelper?.setStrokeColor(currentColor)
            updateLimitRect()
            touchHelper?.openRawDrawing()
            Timber.d("openRawDrawing called")
            touchHelper?.setStrokeStyle(if (currentIsMarker) TouchHelper.STROKE_STYLE_MARKER else TouchHelper.STROKE_STYLE_PENCIL)
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
            Timber.d("setRawDrawingEnabled(true) done")
        } catch (e: Throwable) {
            Timber.w(e, "TouchHelper.create FAILED — using MotionEvent fallback")
            touchHelper = null
            isBooxDevice = false
        }
    }

    fun setTool(color: Int, strokeWidth: Float, isMarker: Boolean) {
        currentColor = color
        currentStrokeWidth = strokeWidth
        currentIsMarker = isMarker
        touchHelper?.setStrokeColor(color)
        touchHelper?.setStrokeWidth(strokeWidth)
        touchHelper?.setStrokeStyle(if (isMarker) TouchHelper.STROKE_STYLE_MARKER else TouchHelper.STROKE_STYLE_PENCIL)
    }

    private fun updateLimitRect() {
        val th = touchHelper ?: return
        val rect = Rect(0, 0, width, height)
        Timber.d("updateLimitRect (local): $rect")
        th.setLimitRect(rect, ArrayList())
    }

    fun getStrokesJson(): String {
        if (strokes.isEmpty()) return ""
        return try { strokesAdapter.toJson(strokes) ?: "" } catch (_: Exception) { "" }
    }

    fun setStrokesJson(json: String) {
        strokes.clear()
        if (json.isNotBlank() && json.startsWith("[")) {
            try { strokesAdapter.fromJson(json)?.let { strokes.addAll(it) } } catch (_: Exception) {}
        }
        Timber.d("setStrokesJson: loaded ${strokes.size} strokes")
        post { redrawAll() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }

        val toolType = event.getToolType(0)
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS &&
            toolType != MotionEvent.TOOL_TYPE_FINGER &&
            toolType != MotionEvent.TOOL_TYPE_ERASER) return true

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
                        strokePoints = activeFallbackPoints.toList(),
                        color = currentColor,
                        strokeWidth = currentStrokeWidth,
                        isMarker = currentIsMarker
                    )
                    strokes.add(stroke)
                    activeFallbackPoints.clear()
                    redrawAll()
                }
            }
        }
        return true
    }

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
            strokePaint.color = currentColor
            strokePaint.strokeWidth = currentStrokeWidth
            strokePaint.xfermode = if (currentIsMarker) markerXfermode else null
            drawPointList(canvas, activeFallbackPoints)
            strokePaint.xfermode = null
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        strokePaint.color = stroke.color
        strokePaint.strokeWidth = stroke.strokeWidth
        strokePaint.xfermode = if (stroke.isMarker) markerXfermode else null
        drawPointList(canvas, stroke.strokePoints)
        strokePaint.xfermode = null
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
