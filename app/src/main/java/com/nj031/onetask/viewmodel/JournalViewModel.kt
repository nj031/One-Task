package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalRepository
import java.time.LocalDate
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

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val notesForSelectedDate: StateFlow<List<JournalNoteEntity>> =
        combine(repository.observeActiveNotes(), _selectedDate) { notes, date ->
            notes.filter { it.journalDate == date.toEpochDay() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun createNote(title: String, content: String) {
        viewModelScope.launch {
            repository.createNote(
                title = title,
                content = content,
                journalDate = _selectedDate.value.toEpochDay()
            )
        }
    }

    fun getNoteById(id: String?): JournalNoteEntity? =
        id?.let { targetId -> notesForSelectedDate.value.find { it.id == targetId } }

    fun updateNote(note: JournalNoteEntity, title: String, content: String) {
        viewModelScope.launch {
            repository.updateNote(note, title, content)
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
}
