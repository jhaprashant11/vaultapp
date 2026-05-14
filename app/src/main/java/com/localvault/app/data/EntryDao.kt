package com.localvault.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PasswordEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: String): PasswordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PasswordEntry)

    @Delete
    suspend fun delete(entry: PasswordEntry)

    @Query("SELECT * FROM entries WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<PasswordEntry>>
}
