package com.groundwork.programmieramt.ui.today

enum class TodayEntryType { SOFORT, TEAM_NOTE, ONE_ON_ONE, MEETING, FREE_NOTE }

data class TodayEntry(
    val type: TodayEntryType,
    val id: Long,
    val time: Long,
    val contextText: String
)
