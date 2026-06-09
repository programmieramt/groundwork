package com.groundwork.programmieramt.pen

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.UUID

class UuidAdapter {
    @ToJson fun toJson(value: UUID): String = value.toString()
    @FromJson fun fromJson(value: String): UUID = UUID.fromString(value)
}
