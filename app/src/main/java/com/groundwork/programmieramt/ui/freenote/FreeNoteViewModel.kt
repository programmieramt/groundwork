package com.groundwork.programmieramt.ui.freenote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.FreeNoteDao
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import com.groundwork.programmieramt.fi.SyncManager
import com.groundwork.programmieramt.fi.UltrabridgeExporter
import com.groundwork.programmieramt.pen.FormTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreeNoteViewModel @Inject constructor(
    private val dao: FreeNoteDao,
    private val syncManager: SyncManager,
    private val ultrabridgeExporter: UltrabridgeExporter
) : ViewModel() {

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getNoteById(id: Long): FreeNoteEntity? = dao.getById(id)

    fun save(entity: FreeNoteEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        val saved = if (entity.id == 0L) entity.copy(id = id) else entity
        syncManager.uploadFreeNote(saved)
        ultrabridgeExporter.export("free_notes", saved.id, saved.strokes) { canvas, w, h -> FormTemplate.drawFreeNote(canvas, w, h) }
    }

    fun delete(entity: FreeNoteEntity) = viewModelScope.launch {
        dao.delete(entity)
        syncManager.deleteFreeNote(entity.id)
    }

    fun syncAll(onDone: () -> Unit) = viewModelScope.launch {
        syncManager.syncAll()
        onDone()
    }
}
