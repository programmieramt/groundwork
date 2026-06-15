package com.groundwork.programmieramt.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Long.toGermanDate(): String =
    if (this == 0L) "" else SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(this))

fun Long.toGermanTime(): String =
    if (this == 0L) "" else SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(this))

fun Long.isToday(): Boolean {
    val day = Calendar.getInstance().apply { timeInMillis = this@isToday }
    val today = Calendar.getInstance()
    return day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}
