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

/** The formatting options the Note Editor's Bold/Italic/Underline/Aa controls apply to a TEXT
 * note's [JournalNoteEntity.content]. BOLD/ITALIC/UNDERLINE are independent per-character styles
 * that can freely overlap each other and a HEADING_* style; HEADING_MEDIUM and HEADING_LARGE are
 * mutually exclusive block-size styles - a range covered by neither is simply the note's normal
 * body text size (the Aa control's "Small" option, so there is no explicit NORMAL style to
 * store). */
enum class NoteFormatStyle { BOLD, ITALIC, UNDERLINE, HEADING_MEDIUM, HEADING_LARGE }

/** One inline formatting range over [JournalNoteEntity.content] - [start] inclusive, [end]
 * exclusive, the same convention [androidx.compose.ui.text.TextRange] uses. */
data class NoteFormatSpan(
    val start: Int,
    val end: Int,
    val style: NoteFormatStyle
)

/** The kind of content one [NoteBlock] holds - see [NoteBlock]'s own doc comment for the mixed-
 * content model this is part of. */
enum class NoteBlockType { TEXT, BULLET, CHECKLIST, IMAGE }

/** One ordered piece of a note's mixed content ([JournalNoteEntity.blocks]) - a note is a plain
 * ordered list of these, so normal text, bullet items, checklist items, and (up to one, for now)
 * image can coexist in any order and survive a round trip through Room/Firestore intact, rather
 * than being flattened into one plain-text field or encoded as special characters inside it.
 *
 * [text]/[formatSpans] are meaningful for TEXT/BULLET/CHECKLIST (empty/unused for IMAGE); a TEXT
 * block's own [text] can contain internal newlines (a multi-line paragraph) the way the Note
 * Editor's old single content field always could, while a BULLET/CHECKLIST block is always exactly
 * one line - pressing Enter inside one always creates a new block rather than a literal newline.
 * [checked] is meaningful only for CHECKLIST. [imagePath] (meaningful only for IMAGE) is a local
 * device file path - never synced verbatim to Firestore, which only ever records that this block
 * IS an image (see [com.nj031.onetask.data.sync.CloudBackupRepository]'s own note-image Storage
 * path); the actual bytes live in Firebase Storage at a path deterministic from the note's own id,
 * the same convention already used for the account's profile photo. */
data class NoteBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: NoteBlockType,
    val text: String = "",
    val checked: Boolean = false,
    val formatSpans: List<NoteFormatSpan> = emptyList(),
    val imagePath: String? = null
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
    val label: String? = null,
    // Bold/Italic/Underline/Aa formatting ranges over [content] - see [NoteFormatSpan]. Only
    // meaningful for a TEXT note; always empty for a CHECKLIST note (its own per-item text isn't
    // formattable). Empty for every note saved before the Note Editor's formatting toolbar
    // existed, which is exactly the correct "no formatting applied" state for them.
    val contentFormatSpans: List<NoteFormatSpan> = emptyList(),
    // Whether this note shows in the Notes list's separate "Pinned Notes" section, above every
    // unpinned note. False for every note saved before pinning existed, which is exactly the
    // correct "not pinned" state for them.
    val pinned: Boolean = false,
    // The note's mixed content - see [NoteBlock]'s own doc comment. Empty for every note saved
    // before the mixed-content editor existed; the Note Editor synthesizes an equivalent block
    // list from [content]/[checklistItems] the first time such a note is opened, rather than this
    // being empty ever being treated as "no content" for an old note. [content]/[checklistItems]
    // themselves keep being written (derived from [blocks]) whenever a note WITH blocks is saved,
    // purely so the Notes list's existing preview/search code keeps working unchanged.
    val blocks: List<NoteBlock> = emptyList()
)

/** The set of labels available to assign to notes - conceptually the Notes equivalent of
 * [com.nj031.onetask.data.task.TaskTagEntity], but kept in its own table since a label is never
 * shared with tasks. */
@Entity(tableName = "note_labels")
data class NoteLabelEntity(
    @PrimaryKey val name: String
)
