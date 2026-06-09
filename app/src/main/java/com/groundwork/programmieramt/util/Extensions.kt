package com.groundwork.programmieramt.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toGermanDate(): String =
    if (this == 0L) "" else SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(this))
