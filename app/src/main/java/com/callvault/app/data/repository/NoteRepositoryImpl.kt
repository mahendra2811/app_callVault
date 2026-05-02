package com.callvault.app.data.repository

import com.callvault.app.data.local.dao.NoteDao
import com.callvault.app.data.local.entity.NoteHistoryEntity
import com.callvault.app.data.local.mapper.toDomain
import com.callvault.app.data.local.mapper.toEntity
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {

    override fun observeForCall(callId: Long): Flow<List<Note>> =
        dao.observeForCall(callId).map { it.map { e -> e.toDomain() } }

    override fun observeForNumber(number: String): Flow<List<Note>> =
        dao.observeForNumber(number).map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(note: Note): Long {
        val existing = if (note.id != 0L) dao.getById(note.id) else null
        if (existing != null && existing.content != note.content) {
            dao.insertHistory(
                NoteHistoryEntity(
                    noteId = existing.id,
                    previousContent = existing.content
                )
            )
            dao.pruneHistory(noteId = existing.id, keep = MAX_HISTORY_PER_NOTE)
        }
        return dao.insert(note.toEntity())
    }

    override suspend fun delete(note: Note) = dao.delete(note.toEntity())

    override suspend fun deleteForNumber(normalizedNumber: String) {
        val notes = dao.observeForNumber(normalizedNumber).first()
        notes.forEach { dao.delete(it) }
    }

    override suspend fun deleteForCall(callId: Long) = dao.deleteForCall(callId)

    private companion object {
        const val MAX_HISTORY_PER_NOTE = 5
    }

    override suspend fun search(query: String): List<Note> {
        val fts = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "${it.replace("\"", "\"\"")}*" }
        if (fts.isBlank()) return emptyList()
        return dao.searchFts(fts).map { it.toDomain() }
    }
}
