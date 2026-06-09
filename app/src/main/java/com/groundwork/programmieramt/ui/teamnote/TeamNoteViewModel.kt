package com.groundwork.programmieramt.ui.teamnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamNoteViewModel @Inject constructor(private val dao: TeamNoteDao) : ViewModel() {

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getNoteById(id: Long): TeamNoteEntity? = dao.getById(id)

    fun save(entity: TeamNoteEntity) = viewModelScope.launch { dao.insert(entity) }

    fun delete(entity: TeamNoteEntity) = viewModelScope.launch { dao.delete(entity) }
}
