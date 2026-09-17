package com.nj031.onetask.data.journal

import androidx.room.TypeConverter

// Control characters (never typeable from a keyboard), the same delimiter convention
// TaskConverters already uses for Subtask - keeps this dependency-free (no JSON library) while
// staying safe against item text containing ordinary punctuation.
private const val CHECKLIST_ITEM_DELIMITER = ""
private const val CHECKLIST_FIELD_DELIMITER = ""

class Converters {
    @TypeConverter
    fun fromStatus(status: JournalNoteStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): JournalNoteStatus = JournalNoteStatus.valueOf(value)

    @TypeConverter
    fun fromNoteType(type: JournalNoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): JournalNoteType =
        runCatching { JournalNoteType.valueOf(value) }.getOrDefault(JournalNoteType.TEXT)

    @TypeConverter
    fun fromChecklistItems(items: List<ChecklistItem>): String =
        items.joinToString(CHECKLIST_ITEM_DELIMITER) { item ->
            listOf(item.id, item.text, item.checked.toString()).joinToString(CHECKLIST_FIELD_DELIMITER)
        }

    @TypeConverter
    fun toChecklistItems(value: String): List<ChecklistItem> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(CHECKLIST_ITEM_DELIMITER).map { entry ->
                val parts = entry.split(CHECKLIST_FIELD_DELIMITER)
                ChecklistItem(id = parts[0], text = parts[1], checked = parts[2].toBoolean())
            }
        }
}
