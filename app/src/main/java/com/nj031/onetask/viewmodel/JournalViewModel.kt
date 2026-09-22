package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.journal.JournalRepository
import com.nj031.onetask.data.journal.NoteFormatSpan
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.settings.NotesViewMode
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.data.sync.CloudGeneralSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository(
        AppDatabase.getInstance(application).journalNoteDao()
    )
    private val settingsRepository = GeneralSettingsRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(settingsRepository.getNotesViewMode())
    val viewMode: StateFlow<NotesViewMode> = _viewMode.asStateFlow()

    /** Every active note, unfiltered by search - the source [notes] and [getNoteById] both
     * read from, so opening a note for editing never depends on the current search text. */
    private val activeNotes: StateFlow<List<JournalNoteEntity>> =
        repository.observeActiveNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Active notes, most-recently-edited first, filtered by [searchQuery] - the single source
     * both List View and Card View render, so switching views never changes which notes show or
     * their order, and searching never changes which view is selected. */
    val notes: StateFlow<List<JournalNoteEntity>> =
        combine(activeNotes, _searchQuery) { allNotes, query ->
            allNotes
                .filter { it.matchesSearch(query) }
                .sortedByDescending { it.updatedAt }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<JournalNoteEntity>> =
        repository.observeArchivedNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<JournalNoteEntity>> =
        repository.observeTrashedNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labels: StateFlow<List<String>> =
        repository.observeLabels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLabel(name: String) {
        viewModelScope.launch {
            repository.addLabel(name)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setViewMode(mode: NotesViewMode) {
        _viewMode.value = mode
        settingsRepository.setNotesViewMode(mode)
        // Notes View Mode is one field of the same account-owned General Settings snapshot
        // GeneralSettingsViewModel's own setters push - see that class's pushToCloud() comment.
        // Pushed as the full current snapshot (not just this one field), matching how the local
        // file already stores every General Setting as one unit.
        viewModelScope.launch {
            val snapshot = settingsRepository.getSnapshot()
            CloudBackupRepository.pushGeneralSettings(
                CloudGeneralSettings(
                    startScreen = snapshot.startScreen.name,
                    defaultTimerMinutes = snapshot.defaultTimerMinutes,
                    defaultTag = snapshot.defaultTag,
                    defaultPostponeIfIncomplete = snapshot.defaultPostponeIfIncomplete,
                    focusSessionNotificationsEnabled = snapshot.focusSessionNotificationsEnabled,
                    focusSessionCompleteEnabled = snapshot.focusSessionCompleteEnabled,
                    weekStartDay = snapshot.weekStartDay.name,
                    timeFormat = snapshot.timeFormat.name,
                    hapticFeedbackEnabled = snapshot.hapticFeedbackEnabled,
                    notesViewMode = snapshot.notesViewMode.name,
                    updatedAt = snapshot.updatedAt
                )
            )
        }
    }

    fun createNote(
        title: String,
        content: String,
        noteType: JournalNoteType,
        checklistItems: List<ChecklistItem>,
        label: String? = null,
        contentFormatSpans: List<NoteFormatSpan> = emptyList(),
        pinned: Boolean = false
    ) {
        viewModelScope.launch {
            repository.createNote(
                title = title,
                content = content,
                noteType = noteType,
                checklistItems = checklistItems,
                label = label,
                contentFormatSpans = contentFormatSpans,
                pinned = pinned
            )
        }
    }

    fun getNoteById(id: String?): JournalNoteEntity? =
        id?.let { targetId -> activeNotes.value.find { it.id == targetId } }

    fun updateNote(
        note: JournalNoteEntity,
        title: String,
        content: String,
        checklistItems: List<ChecklistItem>,
        label: String? = note.label,
        contentFormatSpans: List<NoteFormatSpan> = note.contentFormatSpans,
        pinned: Boolean = note.pinned
    ) {
        viewModelScope.launch {
            repository.updateNote(note, title, content, checklistItems, label, contentFormatSpans, pinned)
        }
    }

    /** Immediate write (not deferred through the Note Editor's own commitOnExit save) - see
     * [JournalRepository.setPinned]'s own doc comment for why Pin/Unpin needs this rather than
     * the label/formatting fields' deferred-save pattern. */
    fun setPinned(note: JournalNoteEntity, pinned: Boolean) {
        viewModelScope.launch {
            repository.setPinned(note, pinned)
        }
    }

    /** Discards a note that auto-save determined no longer has any content worth keeping (e.g.
     * the user cleared both the title and the only checklist item) - a hard delete, not a trash,
     * since the note was never meaningfully filled in to begin with. */
    fun deleteEmptyNote(note: JournalNoteEntity) {
        viewModelScope.launch {
            repository.deleteNotePermanently(note)
        }
    }

    fun archiveNote(note: JournalNoteEntity) {
        viewModelScope.launch {
            repository.archiveNote(note)
        }
    }

    fun trashNote(note: JournalNoteEntity) {
        viewModelScope.launch {
            repository.trashNote(note)
        }
    }

    fun restoreNote(note: JournalNoteEntity) {
        viewModelScope.launch {
            repository.restoreNote(note)
        }
    }

    fun deleteNotePermanently(note: JournalNoteEntity) {
        viewModelScope.launch {
            repository.deleteNotePermanently(note)
        }
    }
}

private fun JournalNoteEntity.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    if (title.contains(query, ignoreCase = true)) return true
    return when (noteType) {
        JournalNoteType.TEXT -> content.contains(query, ignoreCase = true)
        JournalNoteType.CHECKLIST -> checklistItems.any { it.text.contains(query, ignoreCase = true) }
    }
}
