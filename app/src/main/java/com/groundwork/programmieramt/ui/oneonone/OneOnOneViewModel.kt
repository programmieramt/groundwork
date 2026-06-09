package com.groundwork.programmieramt.ui.oneonone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.fi.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionWithMember(
    val session: OneOnOneSessionEntity,
    val memberName: String
)

@HiltViewModel
class OneOnOneViewModel @Inject constructor(
    private val sessionDao: OneOnOneSessionDao,
    private val memberDao: TeamMemberDao,
    private val syncManager: SyncManager
) : ViewModel() {

    val sessions = combine(sessionDao.getAll(), memberDao.getAll()) { sessions, members ->
        val nameMap = members.associateBy({ it.id }, { it.name })
        sessions.map { SessionWithMember(it, nameMap[it.teamMemberId] ?: "?") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members = memberDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getSessionById(id: Long): OneOnOneSessionEntity? = sessionDao.getById(id)

    suspend fun countByMember(memberId: Long): Int = sessionDao.countByMember(memberId)

    suspend fun insertMember(entity: TeamMemberEntity): Long = memberDao.insert(entity)

    fun save(entity: OneOnOneSessionEntity) = viewModelScope.launch {
        val id = sessionDao.insert(entity)
        syncManager.uploadSession(if (entity.id == 0L) entity.copy(id = id) else entity)
    }

    fun delete(entity: OneOnOneSessionEntity) = viewModelScope.launch { sessionDao.delete(entity) }
}
