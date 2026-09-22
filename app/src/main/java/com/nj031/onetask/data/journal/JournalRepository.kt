package com.nj031.onetask.data.journal

import com.nj031.onetask.data.sync.CloudBackupRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val dao: JournalNoteDao) {
    fun observeActiveNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.ACTIVE)

    fun observeArchivedNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.ARCHIVED)

    fun observeTrashedNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.TRASHED)

    suspend fun getAllNotesOnce(): List<JournalNoteEntity> = dao.getAllOnce()

    fun observeLabels(): Flow<List<String>> = dao.getLabels()

    suspend fun getAllLabelsOnce(): List<String> = dao.getLabelsOnce()

    suspend fun addLabel(name: String) {
        dao.insertLabel(NoteLabelEntity(name = name))
        CloudBackupRepository.pushLabel(name)
    }

    suspend fun createNote(
        title: String,
        content: String,
        noteType: JournalNoteType,
        checklistItems: List<ChecklistItem>,
        label: String? = null,
        contentFormatSpans: List<NoteFormatSpan> = emptyList(),
        pinned: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val note = JournalNoteEntity(
            title = title,
            content = content,
            noteType = noteType,
            checklistItems = checklistItems,
            // The Notes screen no longer has a date concept of its own; journalDate is kept on
            // the entity only for backward compatibility with rows saved before this redesign.
            journalDate = LocalDate.now().toEpochDay(),
            createdAt = now,
            updatedAt = now,
            label = label,
            contentFormatSpans = contentFormatSpans,
            pinned = pinned
        )
        dao.insert(note)
        CloudBackupRepository.pushNote(note)
    }

    suspend fun updateNote(
        note: JournalNoteEntity,
        title: String,
        content: String,
        checklistItems: List<ChecklistItem>,
        label: String? = note.label,
        contentFormatSpans: List<NoteFormatSpan> = note.contentFormatSpans,
        pinned: Boolean = note.pinned
    ) {
        val updated = note.copy(
            title = title,
            content = content,
            checklistItems = checklistItems,
            label = label,
            contentFormatSpans = contentFormatSpans,
            pinned = pinned,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(updated)
        CloudBackupRepository.pushNote(updated)
    }

    /** Immediate (not deferred-saved) write, mirroring [archiveNote]/[trashNote] - Pin/Unpin must
     * survive a process death right after tapping it, not just a normal exit through the Note
     * Editor's own deferred commitOnExit save (see NoteEditorScreen's own togglePin). Deliberately
     * does not touch updatedAt, matching archiveNote/trashNote's own status-change convention -
     * pinning is a metadata action, not a content edit. */
    suspend fun setPinned(note: JournalNoteEntity, pinned: Boolean) {
        val updated = note.copy(pinned = pinned)
        dao.update(updated)
        CloudBackupRepository.pushNote(updated)
    }

    suspend fun archiveNote(note: JournalNoteEntity) {
        val updated = note.copy(status = JournalNoteStatus.ARCHIVED)
        dao.update(updated)
        CloudBackupRepository.pushNote(updated)
    }

    suspend fun trashNote(note: JournalNoteEntity) {
        val updated = note.copy(status = JournalNoteStatus.TRASHED)
        dao.update(updated)
        CloudBackupRepository.pushNote(updated)
    }

    suspend fun restoreNote(note: JournalNoteEntity) {
        val updated = note.copy(status = JournalNoteStatus.ACTIVE)
        dao.update(updated)
        CloudBackupRepository.pushNote(updated)
    }

    suspend fun deleteNotePermanently(note: JournalNoteEntity) {
        dao.delete(note)
        CloudBackupRepository.deleteNote(note.id)
    }

    /** Upserts (by id) the given notes into local storage and mirrors them to the cloud
     * backup, without touching any existing note not present in [notes]. Used by Data &
     * Privacy's Restore flow, which merges a backup file into the current account rather than
     * replacing it. */
    suspend fun restoreNotes(notes: List<JournalNoteEntity>) {
        notes.forEach { note ->
            dao.insert(note)
            CloudBackupRepository.pushNote(note)
        }
    }

    /** Permanently deletes every note, locally and from the cloud backup. Does not touch the
     * account itself. */
    suspend fun deleteAllNotes() {
        val allNotes = dao.getAllOnce()
        dao.deleteAllNotes()
        allNotes.forEach { CloudBackupRepository.deleteNote(it.id) }
    }
}
