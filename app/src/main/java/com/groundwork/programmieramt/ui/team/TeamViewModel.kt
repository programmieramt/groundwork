package com.groundwork.programmieramt.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.fi.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val dao: TeamMemberDao,
    private val syncManager: SyncManager
) : ViewModel() {

    val members = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getMemberById(id: Long): TeamMemberEntity? = dao.getById(id)

    fun save(entity: TeamMemberEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        syncManager.uploadTeamMember(if (entity.id == 0L) entity.copy(id = id) else entity)
    }

    fun delete(entity: TeamMemberEntity) = viewModelScope.launch {
        dao.delete(entity)
    }
}
