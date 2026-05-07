package com.callNest.app.data.repository

import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.data.local.mapper.toEntity
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactMetaDao
) : ContactRepository {

    override fun observeAll(): Flow<List<ContactMeta>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeMyContacts(): Flow<List<ContactMeta>> =
        dao.observeMyContacts().map { it.map { e -> e.toDomain() } }

    override fun observeUnsaved(): Flow<List<ContactMeta>> =
        dao.observeUnsaved().map { it.map { e -> e.toDomain() } }

    override fun observeAutoSaved(): Flow<List<ContactMeta>> =
        dao.observeAutoSaved().map { it.map { e -> e.toDomain() } }

    override fun observeByNumber(number: String): Flow<ContactMeta?> =
        dao.observeByNumber(number).map { it?.toDomain() }

    override suspend fun getByNumber(number: String): ContactMeta? =
        dao.getByNumber(number)?.toDomain()

    override suspend fun upsert(meta: ContactMeta) = dao.upsert(meta.toEntity())

    override suspend fun setAutoSaved(number: String, flag: Boolean, format: String?) {
        dao.setAutoSaved(
            number = number,
            flag = flag,
            ts = if (flag) System.currentTimeMillis() else null,
            format = format
        )
    }
}
