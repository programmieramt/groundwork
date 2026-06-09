package com.groundwork.programmieramt.pen

import com.groundwork.programmieramt.da.Stroke
import com.groundwork.programmieramt.da.StrokePoint
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.data.TouchPointList
import timber.log.Timber
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BooxPenCallback(
    private val onStrokeAdded: (Stroke) -> Unit,
    private val onStrokeErased: (UUID) -> Unit,
    private val currentStrokes: () -> List<Stroke>
) : RawInputCallback() {

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = abs(x1 - x2).toDouble()
        val dy = abs(y1 - y2).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint) {}
    override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint) {}
    override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint) {}

    override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
        if (touchPointList.size() == 0) return

        val points = mutableListOf<StrokePoint>()
        var prev = touchPointList[0]
        points.add(StrokePoint(
            (10 * prev.x).roundToInt() / 10.0f,
            (10 * prev.y).roundToInt() / 10.0f,
            (10 * prev.pressure).roundToInt() / 10.0f
        ))

        for (tp in touchPointList) {
            val d = distance(tp.x, tp.y, prev.x, prev.y)
            if (d > 3.0 && d <= 30.0) {
                prev = tp
                points.add(StrokePoint(
                    (10 * tp.x).roundToInt() / 10.0f,
                    (10 * tp.y).roundToInt() / 10.0f,
                    (10 * tp.pressure).roundToInt() / 10.0f
                ))
            }
        }

        onStrokeAdded(Stroke(
            strokeId = UUID.randomUUID(),
            timestamp = System.currentTimeMillis(),
            strokePoints = points
        ))
    }

    override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint) {}
    override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint) {}
    override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {}

    override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList) {
        if (touchPointList.size() == 0) return

        val eraserPoints = mutableListOf<TouchPoint>()
        var prev = touchPointList[0]
        eraserPoints.add(prev)
        for (tp in touchPointList) {
            if (distance(tp.x, tp.y, prev.x, prev.y) > 5.0) {
                prev = tp
                eraserPoints.add(prev)
            }
        }

        for (ep in eraserPoints) {
            for (stroke in currentStrokes()) {
                for (sp in stroke.strokePoints) {
                    if (distance(ep.x, ep.y, sp.x, sp.y) <= 10.0) {
                        Timber.d("Erasing stroke ${stroke.strokeId}")
                        onStrokeErased(stroke.strokeId)
                        return
                    }
                }
            }
        }
    }
}
