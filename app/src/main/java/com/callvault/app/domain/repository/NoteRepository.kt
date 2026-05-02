package com.callvault.app.domain.repository

import com.callvault.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeForCall(callId: Long): Flow<List<Note>>
    fun observeForNumber(number: String): Flow<List<Note>>
    suspend fun upsert(note: Note): Long
    suspend fun delete(note: Note)
    suspend fun deleteForNumber(normalizedNumber: String)
    suspend fun deleteForCall(callId: Long)
    suspend fun search(query: String): List<Note>
}
