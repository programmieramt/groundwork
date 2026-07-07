package com.groundwork.programmieramt.ui.oneonone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.OneOnOneDao
import com.groundwork.programmieramt.db.entity.OneOnOneSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OneOnOneViewModel @Inject constructor(
    private val dao: OneOnOneDao
) : ViewModel() {

    val sessions: StateFlow<List<OneOnOneSession>> = dao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getById(id: Long): OneOnOneSession? = dao.getById(id)

    suspend fun save(session: OneOnOneSession): Long = dao.upsert(session)

    fun delete(id: Long) = viewModelScope.launch { dao.deleteById(id) }
}
