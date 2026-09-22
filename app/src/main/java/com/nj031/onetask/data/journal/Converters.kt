package com.nj031.onetask.data.journal

import androidx.room.TypeConverter

// Control characters (never typeable from a keyboard), the same delimiter convention
// TaskConverters already uses for Subtask - keeps this dependency-free (no JSON library) while
// staying safe against item text containing ordinary punctuation.
private const val CHECKLIST_ITEM_DELIMITER = ""
private const val CHECKLIST_FIELD_DELIMITER = ""
private const val FORMAT_SPAN_DELIMITER = ""
private const val FORMAT_SPAN_FIELD_DELIMITER = ""
// Distinct control characters from the two pairs above, so a block's own encoded fields
// (which embed a nested fromFormatSpans/toFormatSpans-encoded string as one of them) can never
// collide with that nested encoding's own delimiters.
private const val BLOCK_DELIMITER = ""
private const val BLOCK_FIELD_DELIMITER = ""

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

    @TypeConverter
    fun fromFormatSpans(spans: List<NoteFormatSpan>): String =
        spans.joinToString(FORMAT_SPAN_DELIMITER) { span ->
            listOf(span.start.toString(), span.end.toString(), span.style.name)
                .joinToString(FORMAT_SPAN_FIELD_DELIMITER)
        }

    @TypeConverter
    fun toFormatSpans(value: String): List<NoteFormatSpan> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(FORMAT_SPAN_DELIMITER).mapNotNull { entry ->
                val parts = entry.split(FORMAT_SPAN_FIELD_DELIMITER)
                val start = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val end = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val style = parts.getOrNull(2)?.let { runCatching { NoteFormatStyle.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                NoteFormatSpan(start = start, end = end, style = style)
            }
        }

    @TypeConverter
    fun fromNoteBlockType(type: NoteBlockType): String = type.name

    @TypeConverter
    fun toNoteBlockType(value: String): NoteBlockType =
        runCatching { NoteBlockType.valueOf(value) }.getOrDefault(NoteBlockType.TEXT)

    @TypeConverter
    fun fromNoteBlocks(blocks: List<NoteBlock>): String =
        blocks.joinToString(BLOCK_DELIMITER) { block ->
            listOf(
                block.id,
                block.type.name,
                block.text,
                block.checked.toString(),
                fromFormatSpans(block.formatSpans),
                block.imagePath.orEmpty()
            ).joinToString(BLOCK_FIELD_DELIMITER)
        }

    @TypeConverter
    fun toNoteBlocks(value: String): List<NoteBlock> =
        if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(BLOCK_DELIMITER).mapNotNull { entry ->
                val parts = entry.split(BLOCK_FIELD_DELIMITER)
                val id = parts.getOrNull(0) ?: return@mapNotNull null
                val type = parts.getOrNull(1)?.let { runCatching { NoteBlockType.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val text = parts.getOrNull(2) ?: ""
                val checked = parts.getOrNull(3)?.toBoolean() ?: false
                val formatSpans = parts.getOrNull(4)?.let { toFormatSpans(it) } ?: emptyList()
                val imagePath = parts.getOrNull(5)?.takeIf { it.isNotEmpty() }
                NoteBlock(id = id, type = type, text = text, checked = checked, formatSpans = formatSpans, imagePath = imagePath)
            }
        }
}
