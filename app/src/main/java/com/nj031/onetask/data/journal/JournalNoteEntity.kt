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
    val status: JournalNoteStatus = JournalNoteStatus.ACTIVE,
    // Null means no label assigned. A single nullable field (rather than a list) is what
    // structurally enforces "a note can have at most one label" - there's no representation for
    // more than one.
    val label: String? = null
)

/** The set of labels available to assign to notes - conceptually the Notes equivalent of
 * [com.nj031.onetask.data.task.TaskTagEntity], but kept in its own table since a label is never
 * shared with tasks. */
@Entity(tableName = "note_labels")
data class NoteLabelEntity(
    @PrimaryKey val name: String
)
