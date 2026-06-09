package com.groundwork.programmieramt.fi

import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val client: WebDavClient,
    private val moshi: Moshi,
    private val teamMemberDao: TeamMemberDao,
    private val oneOnOneDao: OneOnOneSessionDao,
    private val teamNoteDao: TeamNoteDao,
    private val sofortDao: SofortNoteDao
) {
    private val teamMemberAdapter = moshi.adapter(TeamMemberEntity::class.java)
    private val sessionAdapter = moshi.adapter(OneOnOneSessionEntity::class.java)
    private val teamNoteAdapter = moshi.adapter(TeamNoteEntity::class.java)
    private val sofortAdapter = moshi.adapter(SofortNoteEntity::class.java)

    private fun ensureDirs(): Result<Unit> {
        return client.ensureDirectory("groundwork")
            .flatMap { client.ensureDirectory("groundwork/team_members") }
            .flatMap { client.ensureDirectory("groundwork/sessions") }
            .flatMap { client.ensureDirectory("groundwork/team_notes") }
            .flatMap { client.ensureDirectory("groundwork/sofort_notes") }
    }

    suspend fun uploadTeamMember(entity: TeamMemberEntity) = withContext(Dispatchers.IO) {
        try {
            client.put("groundwork/team_members/${entity.id}.json", teamMemberAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadTeamMember failed")
        }
    }

    suspend fun uploadSession(entity: OneOnOneSessionEntity) = withContext(Dispatchers.IO) {
        try {
            client.put("groundwork/sessions/${entity.id}.json", sessionAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadSession failed")
        }
    }

    suspend fun uploadTeamNote(entity: TeamNoteEntity) = withContext(Dispatchers.IO) {
        try {
            client.put("groundwork/team_notes/${entity.id}.json", teamNoteAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadTeamNote failed")
        }
    }

    suspend fun uploadSofortNote(entity: SofortNoteEntity) = withContext(Dispatchers.IO) {
        try {
            client.put("groundwork/sofort_notes/${entity.id}.json", sofortAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadSofortNote failed")
        }
    }

    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureDirs().getOrThrow()

            val members = teamMemberDao.getAllOnce()
            members.forEach { client.put("groundwork/team_members/${it.id}.json", teamMemberAdapter.toJson(it)) }

            val sessions = oneOnOneDao.getAllOnce()
            sessions.forEach { client.put("groundwork/sessions/${it.id}.json", sessionAdapter.toJson(it)) }

            val teamNotes = teamNoteDao.getAllOnce()
            teamNotes.forEach { client.put("groundwork/team_notes/${it.id}.json", teamNoteAdapter.toJson(it)) }

            val sofortNotes = sofortDao.getAllOnce()
            sofortNotes.forEach { client.put("groundwork/sofort_notes/${it.id}.json", sofortAdapter.toJson(it)) }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "syncAll failed")
            Result.failure(e)
        }
    }

    private fun <T> Result<T>.flatMap(block: (T) -> Result<T>): Result<T> =
        if (isSuccess) block(getOrThrow()) else this
}
