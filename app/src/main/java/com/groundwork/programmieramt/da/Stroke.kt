package com.groundwork.programmieramt.da

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Stroke(
    var strokeId: UUID,
    var timestamp: Long = 0L,
    var strokePoints: List<StrokePoint>,
    var color: Int = -16777216,
    var strokeWidth: Float = 3.0f
)
