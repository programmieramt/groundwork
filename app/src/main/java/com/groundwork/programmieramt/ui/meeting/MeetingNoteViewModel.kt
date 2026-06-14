package com.groundwork.programmieramt.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.MeetingNoteDao
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
import com.groundwork.programmieramt.fi.SyncManager
import com.groundwork.programmieramt.fi.UltrabridgeExporter
import com.groundwork.programmieramt.pen.FormTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingNoteViewModel @Inject constructor(
    private val dao: MeetingNoteDao,
    private val syncManager: SyncManager,
    private val ultrabridgeExporter: UltrabridgeExporter
) : ViewModel() {

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getNoteById(id: Long): MeetingNoteEntity? = dao.getById(id)

    fun save(entity: MeetingNoteEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        val saved = if (entity.id == 0L) entity.copy(id = id) else entity
        syncManager.uploadMeetingNote(saved)
        ultrabridgeExporter.export("meeting_notes", saved.id, saved.strokes) { canvas, w, h -> FormTemplate.drawMeetingNote(canvas, w, h) }
    }

    fun delete(entity: MeetingNoteEntity) = viewModelScope.launch {
        dao.delete(entity)
        syncManager.deleteMeetingNote(entity.id)
    }

    fun syncAll(onDone: () -> Unit) = viewModelScope.launch {
        syncManager.syncAll()
        onDone()
    }
}
