package com.groundwork.programmieramt.da

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StrokePoint(
    var x: Float,
    var y: Float,
    var p: Float,
    var t: Long = 0L
)
