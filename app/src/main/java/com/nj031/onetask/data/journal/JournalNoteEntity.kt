package com.nj031.onetask.data.journal

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class JournalNoteStatus { ACTIVE, ARCHIVED, TRASHED }

@Entity(tableName = "journal_notes")
data class JournalNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val journalDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val status: JournalNoteStatus = JournalNoteStatus.ACTIVE
)
