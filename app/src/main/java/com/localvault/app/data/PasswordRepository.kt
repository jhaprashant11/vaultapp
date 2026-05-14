package com.localvault.app.data

import com.localvault.app.VaultSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class PasswordRepository(private val session: VaultSession) {

    fun observeAll(): Flow<List<PasswordEntry>> =
        session.database.flatMapLatest { db ->
            if (db == null) flowOf(emptyList())
            else db.entryDao().observeAll()
        }

    fun search(query: String): Flow<List<PasswordEntry>> =
        session.database.flatMapLatest { db ->
            if (db == null) flowOf(emptyList())
            else if (query.isBlank()) db.entryDao().observeAll()
            else db.entryDao().search(query.trim())
        }

    suspend fun getById(id: String): PasswordEntry? =
        session.database.value?.entryDao()?.getById(id)

    suspend fun upsert(entry: PasswordEntry) {
        session.database.value?.entryDao()?.upsert(entry)
    }

    suspend fun delete(entry: PasswordEntry) {
        session.database.value?.entryDao()?.delete(entry)
    }
}
