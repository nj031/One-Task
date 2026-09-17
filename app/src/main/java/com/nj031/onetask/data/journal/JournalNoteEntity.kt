package com.nj031.onetask.data.journal

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class JournalNoteStatus { ACTIVE, ARCHIVED, TRASHED }

/** A note is either a plain Text note (its [JournalNoteEntity.content] is freeform text) or a
 * Checklist note (its [JournalNoteEntity.checklistItems] holds the list of items, and
 * [JournalNoteEntity.content] is unused/blank) - fixed at creation, never converted afterward. */
enum class JournalNoteType { TEXT, CHECKLIST }

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean = false
)

@Entity(tableName = "journal_notes")
data class JournalNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val noteType: JournalNoteType = JournalNoteType.TEXT,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val journalDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val status: JournalNoteStatus = JournalNoteStatus.ACTIVE
)
