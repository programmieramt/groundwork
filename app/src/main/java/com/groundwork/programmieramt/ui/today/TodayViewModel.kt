package com.groundwork.programmieramt.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.FreeNoteDao
import com.groundwork.programmieramt.db.dao.MeetingNoteDao
import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.util.isToday
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    sofortDao: SofortNoteDao,
    teamNoteDao: TeamNoteDao,
    sessionDao: OneOnOneSessionDao,
    meetingDao: MeetingNoteDao,
    freeNoteDao: FreeNoteDao,
    memberDao: TeamMemberDao
) : ViewModel() {

    private val plainEntries = combine(
        sofortDao.getAll(),
        teamNoteDao.getAll(),
        meetingDao.getAll(),
        freeNoteDao.getAll()
    ) { sofort, teamNotes, meetings, freeNotes ->
        val entries = mutableListOf<TodayEntry>()
        sofort.filter { it.datum.isToday() }.forEach {
            entries.add(TodayEntry(TodayEntryType.SOFORT, it.id, it.datum, it.kategorie.ifBlank { "Sofort" }))
        }
        teamNotes.filter { it.datum.isToday() }.forEach {
            entries.add(TodayEntry(TodayEntryType.TEAM_NOTE, it.id, it.datum, it.kontextMeeting.ifBlank { "Beobachtung" }))
        }
        meetings.filter { it.datum.isToday() }.forEach {
            entries.add(TodayEntry(TodayEntryType.MEETING, it.id, it.datum, it.teilnehmer.ifBlank { "Meeting" }))
        }
        freeNotes.filter { it.datum.isToday() }.forEach {
            entries.add(TodayEntry(TodayEntryType.FREE_NOTE, it.id, it.datum, "Notiz"))
        }
        entries
    }

    private val sessionEntries = combine(sessionDao.getAll(), memberDao.getAll()) { sessions, members ->
        val nameMap = members.associateBy({ it.id }, { it.name })
        sessions.filter { it.datum.isToday() }.map {
            TodayEntry(TodayEntryType.ONE_ON_ONE, it.id, it.datum, "1:1 mit ${nameMap[it.teamMemberId] ?: "?"}")
        }
    }

    val entries = combine(plainEntries, sessionEntries) { a, b ->
        (a + b).sortedByDescending { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
