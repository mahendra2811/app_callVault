package com.callNest.app.domain.repository

import com.callNest.app.domain.model.ContactMeta
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeAll(): Flow<List<ContactMeta>>
    fun observeMyContacts(): Flow<List<ContactMeta>>
    fun observeUnsaved(): Flow<List<ContactMeta>>
    fun observeAutoSaved(): Flow<List<ContactMeta>>
    fun observeByNumber(number: String): Flow<ContactMeta?>
    suspend fun getByNumber(number: String): ContactMeta?
    suspend fun upsert(meta: ContactMeta)
    suspend fun setAutoSaved(number: String, flag: Boolean, format: String?)
}
