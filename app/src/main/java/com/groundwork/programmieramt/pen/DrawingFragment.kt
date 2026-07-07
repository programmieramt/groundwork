package com.groundwork.programmieramt.pen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.groundwork.programmieramt.da.Stroke
import com.groundwork.programmieramt.da.StrokePoint
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

abstract class DrawingFragment : Fragment(), SurfaceHolder.Callback {

    // Subclass provides the SurfaceView (transparent, on top) and the ImageView (template behind)
    abstract fun provideSurfaceView(): SurfaceView
    abstract fun provideTemplateView(): ImageView
    abstract fun buildTemplateBitmap(width: Int, height: Int): Bitmap

    open fun onStrokesChanged() {}

    protected var touchHelper: TouchHelper? = null
    protected var isBooxDevice = false

    // Stroke state
    private val strokes = mutableListOf<Stroke>()
    private val activePoints = mutableListOf<StrokePoint>()
    private var strokeStart = 0L

    // Current tool
    var currentColor = Color.BLACK
    var currentStrokeWidth = 3f
    var currentIsMarker = false
    var currentIsEraser = false

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val markerXfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

    // All no-ops — Boox SDK handles live ink rendering; strokes are tracked via MotionEvent
    private val rawCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, p: TouchPoint) {}
        override fun onEndRawDrawing(b: Boolean, p: TouchPoint) {}
        override fun onRawDrawingTouchPointMoveReceived(p: TouchPoint) {}
        override fun onRawDrawingTouchPointListReceived(pl: TouchPointList) {}
        override fun onBeginRawErasing(b: Boolean, p: TouchPoint) {}
        override fun onEndRawErasing(b: Boolean, p: TouchPoint) {}
        override fun onRawErasingTouchPointMoveReceived(p: TouchPoint) {}
        override fun onRawErasingTouchPointListReceived(pl: TouchPointList) {}
    }

    // Call from onViewCreated after the SurfaceView is in the hierarchy
    protected fun initSurface() {
        val sv = provideSurfaceView()
        sv.setZOrderOnTop(true)
        sv.holder.setFormat(PixelFormat.TRANSPARENT)
        sv.keepScreenOn = true
        sv.holder.addCallback(this)
        sv.setOnTouchListener { _, event -> handleTouch(event) }
    }

    // region Lifecycle

    override fun onResume() {
        super.onResume()
        if (!isBooxDevice) return
        val sv = provideSurfaceView()
        if (sv.width > 0 && sv.height > 0) {
            touchHelper?.setLimitRect(Rect(0, 0, sv.width, sv.height), ArrayList())
        }
        touchHelper?.openRawDrawing()
        touchHelper?.setStrokeStyle(toolStyle())
        touchHelper?.setStrokeColor(currentColor)
        touchHelper?.setStrokeWidth(currentStrokeWidth)
        touchHelper?.setRawDrawingEnabled(true)
        touchHelper?.isRawDrawingRenderEnabled = true
    }

    override fun onPause() {
        super.onPause()
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        touchHelper?.closeRawDrawing()
    }

    // endregion

    // region SurfaceHolder.Callback

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("surfaceCreated")
        try {
            touchHelper = TouchHelper.create(provideSurfaceView(), rawCallback)
            isBooxDevice = true
            Timber.d("TouchHelper OK — ${android.os.Build.MODEL}")
        } catch (e: Throwable) {
            Timber.w(e, "TouchHelper unavailable — MotionEvent fallback")
            touchHelper = null
            isBooxDevice = false
        }
        clearCanvas()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surfaceChanged ${width}x${height}")
        if (width > 0 && height > 0) {
            provideTemplateView().setImageBitmap(buildTemplateBitmap(width, height))
        }
        if (isBooxDevice) {
            touchHelper?.setLimitRect(Rect(0, 0, width, height), ArrayList())
            touchHelper?.openRawDrawing()
            touchHelper?.setStrokeStyle(toolStyle())
            touchHelper?.setStrokeColor(currentColor)
            touchHelper?.setStrokeWidth(currentStrokeWidth)
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
        }
        applyStrokes()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surfaceDestroyed")
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.isRawDrawingRenderEnabled = false
        touchHelper?.closeRawDrawing()
    }

    // endregion

    // region Drawing

    private fun clearCanvas() {
        val sv = provideSurfaceView()
        val c = sv.holder.lockCanvas() ?: return
        try {
            try { EpdController.enablePost(sv, 1) } catch (_: Throwable) {}
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } finally {
            sv.holder.unlockCanvasAndPost(c)
        }
    }

    fun applyStrokes() {
        val sv = provideSurfaceView()
        val c = sv.holder.lockCanvas() ?: return
        try {
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            for (s in strokes) renderStroke(c, s)
        } finally {
            try { EpdController.enablePost(sv, 1) } catch (_: Throwable) {}
            touchHelper?.setRawDrawingEnabled(false)
            touchHelper?.isRawDrawingRenderEnabled = false
            sv.holder.unlockCanvasAndPost(c)
            touchHelper?.setRawDrawingEnabled(true)
            touchHelper?.isRawDrawingRenderEnabled = true
        }
    }

    private fun renderStroke(canvas: Canvas, s: Stroke) {
        if (s.strokePoints.size < 2) return
        strokePaint.color = s.color
        strokePaint.strokeWidth = s.strokeWidth
        strokePaint.alpha = if (s.isMarker) 100 else 255
        strokePaint.xfermode = if (s.isMarker) markerXfermode else null
        val path = Path()
        path.moveTo(s.strokePoints[0].x, s.strokePoints[0].y)
        var prev = s.strokePoints[0]
        for (i in 1 until s.strokePoints.size) {
            val curr = s.strokePoints[i]
            path.quadTo(prev.x, prev.y, curr.x, curr.y)
            prev = curr
        }
        canvas.drawPath(path, strokePaint)
        strokePaint.xfermode = null
    }

    // endregion

    // region Touch / stroke collection

    private fun handleTouch(event: MotionEvent): Boolean {
        val x = (10 * event.x).roundToInt() / 10f
        val y = (10 * event.y).roundToInt() / 10f
        val pressure = (10 * event.pressure).roundToInt() / 10f

        if (currentIsEraser) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> eraseAt(x, y)
                MotionEvent.ACTION_UP -> onStrokesChanged()
            }
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activePoints.clear()
                strokeStart = System.currentTimeMillis()
                activePoints.add(StrokePoint(x, y, pressure))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    val hx = (10 * event.getHistoricalX(i)).roundToInt() / 10f
                    val hy = (10 * event.getHistoricalY(i)).roundToInt() / 10f
                    val hp = (10 * event.getHistoricalPressure(i)).roundToInt() / 10f
                    val last = activePoints.lastOrNull()
                    if (last == null || dist(hx, hy, last.x, last.y) > 1.0) {
                        activePoints.add(StrokePoint(hx, hy, hp))
                    }
                }
                val last = activePoints.lastOrNull()
                if (last == null || dist(x, y, last.x, last.y) > 1.0) {
                    activePoints.add(StrokePoint(x, y, pressure))
                }
                if (!isBooxDevice) applyStrokes()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activePoints.isNotEmpty()) {
                    strokes.add(Stroke(UUID.randomUUID(), strokeStart,
                        activePoints.toList(), currentColor, currentStrokeWidth, currentIsMarker))
                    activePoints.clear()
                    applyStrokes()
                    onStrokesChanged()
                }
            }
        }
        return true
    }

    private fun eraseAt(x: Float, y: Float) {
        val r = 12f * resources.displayMetrics.density
        val removed = strokes.removeAll { s -> s.strokePoints.any { dist(x, y, it.x, it.y) <= r } }
        if (removed) applyStrokes()
    }

    // endregion

    // region Public API

    fun setTool(color: Int, width: Float, isMarker: Boolean, isEraser: Boolean) {
        currentColor = color
        currentStrokeWidth = width
        currentIsMarker = isMarker
        currentIsEraser = isEraser
        if (!isEraser) {
            touchHelper?.setStrokeColor(color)
            touchHelper?.setStrokeWidth(width)
            touchHelper?.setStrokeStyle(toolStyle())
        }
    }

    fun getStrokesJson(): String {
        if (strokes.isEmpty()) return ""
        return try { adapter.toJson(strokes) ?: "" } catch (_: Exception) { "" }
    }

    fun loadStrokes(json: String) {
        strokes.clear()
        if (json.isNotBlank() && json.startsWith("[")) {
            try { adapter.fromJson(json)?.let { strokes.addAll(it) } } catch (_: Exception) {}
        }
        applyStrokes() // no-op if surface not ready; surfaceChanged will redraw
    }

    // endregion

    private fun toolStyle() =
        if (currentIsMarker) TouchHelper.STROKE_STYLE_MARKER else TouchHelper.STROKE_STYLE_PENCIL

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = abs(x1 - x2).toDouble()
        val dy = abs(y1 - y2).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder().add(UuidAdapter()).build()
        private val type = Types.newParameterizedType(List::class.java, Stroke::class.java)
        private val adapter by lazy { moshi.adapter<List<Stroke>>(type) }
    }
}
