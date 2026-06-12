package com.groundwork.programmieramt.ui.teamnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import com.groundwork.programmieramt.fi.SyncManager
import com.groundwork.programmieramt.fi.UltrabridgeExporter
import com.groundwork.programmieramt.pen.FormTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamNoteViewModel @Inject constructor(
    private val dao: TeamNoteDao,
    private val syncManager: SyncManager,
    private val ultrabridgeExporter: UltrabridgeExporter
) : ViewModel() {

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getNoteById(id: Long): TeamNoteEntity? = dao.getById(id)

    fun save(entity: TeamNoteEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        val saved = if (entity.id == 0L) entity.copy(id = id) else entity
        syncManager.uploadTeamNote(saved)
        ultrabridgeExporter.export("team_notes", saved.id, saved.strokes) { canvas, w, h -> FormTemplate.drawTeamNote(canvas, w, h) }
    }

    fun delete(entity: TeamNoteEntity) = viewModelScope.launch {
        dao.delete(entity)
        syncManager.deleteTeamNote(entity.id)
    }

    fun syncAll(onDone: () -> Unit) = viewModelScope.launch {
        syncManager.syncAll()
        onDone()
    }
}
