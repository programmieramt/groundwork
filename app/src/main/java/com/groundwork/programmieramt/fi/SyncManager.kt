package com.groundwork.programmieramt.fi

import com.groundwork.programmieramt.db.dao.FreeNoteDao
import com.groundwork.programmieramt.db.dao.MeetingNoteDao
import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
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
    private val sofortDao: SofortNoteDao,
    private val freeNoteDao: FreeNoteDao,
    private val meetingNoteDao: MeetingNoteDao
) {
    private val teamMemberAdapter = moshi.adapter(TeamMemberEntity::class.java)
    private val sessionAdapter = moshi.adapter(OneOnOneSessionEntity::class.java)
    private val teamNoteAdapter = moshi.adapter(TeamNoteEntity::class.java)
    private val sofortAdapter = moshi.adapter(SofortNoteEntity::class.java)
    private val freeNoteAdapter = moshi.adapter(FreeNoteEntity::class.java)
    private val meetingNoteAdapter = moshi.adapter(MeetingNoteEntity::class.java)

    @Volatile private var dirsEnsured = false

    private fun ensureDirs(): Result<Unit> =
        client.ensureDirectory("groundwork")
            .flatMap { client.ensureDirectory("groundwork/team_members") }
            .flatMap { client.ensureDirectory("groundwork/sessions") }
            .flatMap { client.ensureDirectory("groundwork/team_notes") }
            .flatMap { client.ensureDirectory("groundwork/sofort_notes") }
            .flatMap { client.ensureDirectory("groundwork/free_notes") }
            .flatMap { client.ensureDirectory("groundwork/meeting_notes") }

    private fun ensureDirsOnce() {
        if (!dirsEnsured) {
            ensureDirs().onSuccess { dirsEnsured = true }
        }
    }

    suspend fun uploadTeamMember(entity: TeamMemberEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/team_members/${entity.id}.json", teamMemberAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadTeamMember failed")
        }
    }

    suspend fun uploadSession(entity: OneOnOneSessionEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/sessions/${entity.id}.json", sessionAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadSession failed")
        }
    }

    suspend fun uploadTeamNote(entity: TeamNoteEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/team_notes/${entity.id}.json", teamNoteAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadTeamNote failed")
        }
    }

    suspend fun uploadSofortNote(entity: SofortNoteEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/sofort_notes/${entity.id}.json", sofortAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadSofortNote failed")
        }
    }

    suspend fun uploadFreeNote(entity: FreeNoteEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/free_notes/${entity.id}.json", freeNoteAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadFreeNote failed")
        }
    }

    suspend fun uploadMeetingNote(entity: MeetingNoteEntity) = withContext(Dispatchers.IO) {
        try {
            ensureDirsOnce()
            client.put("groundwork/meeting_notes/${entity.id}.json", meetingNoteAdapter.toJson(entity))
        } catch (e: Exception) {
            Timber.e(e, "uploadMeetingNote failed")
        }
    }

    suspend fun deleteTeamMember(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/team_members/$id.json")
            .onFailure { Timber.e(it, "deleteTeamMember failed") }
    }

    suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/sessions/$id.json")
            .onFailure { Timber.e(it, "deleteSession failed") }
    }

    suspend fun deleteTeamNote(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/team_notes/$id.json")
            .onFailure { Timber.e(it, "deleteTeamNote failed") }
    }

    suspend fun deleteSofortNote(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/sofort_notes/$id.json")
            .onFailure { Timber.e(it, "deleteSofortNote failed") }
    }

    suspend fun deleteFreeNote(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/free_notes/$id.json")
            .onFailure { Timber.e(it, "deleteFreeNote failed") }
    }

    suspend fun deleteMeetingNote(id: Long) = withContext(Dispatchers.IO) {
        client.delete("groundwork/meeting_notes/$id.json")
            .onFailure { Timber.e(it, "deleteMeetingNote failed") }
    }

    // Push all local data, then pull from server with "newest wins" by updatedAt.
    // Team members are pushed/pulled first since sessions reference them by FK.
    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureDirs().getOrThrow()
            dirsEnsured = true

            teamMemberDao.getAllOnce().forEach { client.put("groundwork/team_members/${it.id}.json", teamMemberAdapter.toJson(it)) }
            oneOnOneDao.getAllOnce().forEach { client.put("groundwork/sessions/${it.id}.json", sessionAdapter.toJson(it)) }
            teamNoteDao.getAllOnce().forEach { client.put("groundwork/team_notes/${it.id}.json", teamNoteAdapter.toJson(it)) }
            sofortDao.getAllOnce().forEach { client.put("groundwork/sofort_notes/${it.id}.json", sofortAdapter.toJson(it)) }
            freeNoteDao.getAllOnce().forEach { client.put("groundwork/free_notes/${it.id}.json", freeNoteAdapter.toJson(it)) }
            meetingNoteDao.getAllOnce().forEach { client.put("groundwork/meeting_notes/${it.id}.json", meetingNoteAdapter.toJson(it)) }

            pullCollection("groundwork/team_members") { json ->
                teamMemberAdapter.fromJson(json)?.let { remote ->
                    val local = teamMemberDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        teamMemberDao.insert(remote)
                    }
                }
            }
            pullCollection("groundwork/sessions") { json ->
                sessionAdapter.fromJson(json)?.let { remote ->
                    val local = oneOnOneDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        oneOnOneDao.insert(remote)
                    }
                }
            }
            pullCollection("groundwork/team_notes") { json ->
                teamNoteAdapter.fromJson(json)?.let { remote ->
                    val local = teamNoteDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        teamNoteDao.insert(remote)
                    }
                }
            }
            pullCollection("groundwork/sofort_notes") { json ->
                sofortAdapter.fromJson(json)?.let { remote ->
                    val local = sofortDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        sofortDao.insert(remote)
                    }
                }
            }
            pullCollection("groundwork/free_notes") { json ->
                freeNoteAdapter.fromJson(json)?.let { remote ->
                    val local = freeNoteDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        freeNoteDao.insert(remote)
                    }
                }
            }
            pullCollection("groundwork/meeting_notes") { json ->
                meetingNoteAdapter.fromJson(json)?.let { remote ->
                    val local = meetingNoteDao.getById(remote.id)
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        meetingNoteDao.insert(remote)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "syncAll failed")
            Result.failure(e)
        }
    }

    private suspend fun pullCollection(path: String, process: suspend (String) -> Unit) {
        val hrefs = client.listFiles(path).getOrDefault(emptyList())
        for (href in hrefs) {
            val filename = href.substringAfterLast('/')
            if (!filename.endsWith(".json")) continue
            val json = client.get("$path/$filename").getOrNull() ?: continue
            try {
                process(json)
            } catch (e: Exception) {
                Timber.e(e, "pullCollection $path/$filename: $e")
            }
        }
    }

    private fun <T> Result<T>.flatMap(block: (T) -> Result<T>): Result<T> =
        if (isSuccess) block(getOrThrow()) else this
}
