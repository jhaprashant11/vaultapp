package com.localvault.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "entries")
data class PasswordEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    @ColumnInfo(defaultValue = "general") val category: String = PasswordCategory.General.id,
    val url: String?,
    val notes: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class PasswordCategory(val id: String, val label: String) {
    Bank("bank", "Bank"),
    Upi("upi", "UPI"),
    Email("email", "Email"),
    Game("game", "Game"),
    General("general", "Other");

    companion object {
        fun fromId(id: String?): PasswordCategory =
            values().firstOrNull { it.id == id } ?: General
    }
}
