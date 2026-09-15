package com.nj031.onetask.data.journal

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val dao: JournalNoteDao) {
    fun observeActiveNotes(): Flow<List<JournalNoteEntity>> =
        dao.getByStatus(JournalNoteStatus.ACTIVE)

    suspend fun createNote(title: String, content: String, journalDate: Long) {
        val now = System.currentTimeMillis()
        dao.insert(
            JournalNoteEntity(
                title = title,
                content = content,
                journalDate = journalDate,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateNote(note: JournalNoteEntity, title: String, content: String) {
        dao.update(
            note.copy(
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun archiveNote(note: JournalNoteEntity) {
        dao.update(note.copy(status = JournalNoteStatus.ARCHIVED))
    }

    suspend fun trashNote(note: JournalNoteEntity) {
        dao.update(note.copy(status = JournalNoteStatus.TRASHED))
    }
}
