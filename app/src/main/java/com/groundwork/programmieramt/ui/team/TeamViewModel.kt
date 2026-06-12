package com.groundwork.programmieramt.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.fi.SyncManager
import com.groundwork.programmieramt.fi.UltrabridgeExporter
import com.groundwork.programmieramt.pen.FormTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val dao: TeamMemberDao,
    private val syncManager: SyncManager,
    private val ultrabridgeExporter: UltrabridgeExporter
) : ViewModel() {

    val members = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getMemberById(id: Long): TeamMemberEntity? = dao.getById(id)

    fun save(entity: TeamMemberEntity) = viewModelScope.launch {
        val id = dao.insert(entity)
        val saved = if (entity.id == 0L) entity.copy(id = id) else entity
        syncManager.uploadTeamMember(saved)
        ultrabridgeExporter.export("team_members", saved.id, saved.strokes) { canvas, w, h -> FormTemplate.drawTeamMember(canvas, w, h) }
    }

    fun delete(entity: TeamMemberEntity) = viewModelScope.launch {
        dao.delete(entity)
        syncManager.deleteTeamMember(entity.id)
    }

    fun syncAll(onDone: () -> Unit) = viewModelScope.launch {
        syncManager.syncAll()
        onDone()
    }
}
