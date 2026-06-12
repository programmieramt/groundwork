package com.groundwork.programmieramt.ui.sofort

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import com.groundwork.programmieramt.fi.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SofortViewModel @Inject constructor(
    private val dao: SofortNoteDao,
    private val syncManager: SyncManager
) : ViewModel() {

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getNoteById(id: Long): SofortNoteEntity? = dao.getById(id)

    fun save(entity: SofortNoteEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        syncManager.uploadSofortNote(if (entity.id == 0L) entity.copy(id = id) else entity)
    }

    fun delete(entity: SofortNoteEntity) = viewModelScope.launch {
        dao.delete(entity)
        syncManager.deleteSofortNote(entity.id)
    }

    fun syncAll(onDone: () -> Unit) = viewModelScope.launch {
        syncManager.syncAll()
        onDone()
    }
}
