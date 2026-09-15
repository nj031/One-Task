package com.nj031.onetask.data.journal

import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val dao: JournalNoteDao) {
    fun observeActiveNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.ACTIVE)

    fun observeArchivedNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.ARCHIVED)

    fun observeTrashedNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.TRASHED)

    fun observeActiveNoteDates(): Flow<List<Long>> =
        dao.getDatesByStatus(JournalNoteStatus.ACTIVE)

    suspend fun createNote(title: String, content: String, journalDate: Long) {
        val now = System.currentTimeMillis()
        val note = JournalNoteEntity(
            title = title,
            content = content,
            journalDate = journalDate,
            createdAt = now,
            updatedAt = now
        )
        dao.insert(note)
        CloudBackupRepository.pushNote(note)
    }

    suspend fun updateNote(note: JournalNoteEntity, title: String, content: String) {
        val updated = note.copy(
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
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
}
