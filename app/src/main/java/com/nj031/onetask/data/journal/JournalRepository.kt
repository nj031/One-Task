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
}
