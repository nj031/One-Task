package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskRepository
import com.nj031.onetask.data.task.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.getInstance(application).taskDao())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val tasksForSelectedDate: StateFlow<List<TaskEntity>> =
        _selectedDate.flatMapLatest { date -> repository.observeTasksByDate(date.toEpochDay()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customTags: StateFlow<List<String>> =
        repository.observeCustomTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.postponeOverdueTasks(LocalDate.now().toEpochDay())
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun createTask(
        name: String,
        subtasks: List<String>,
        timerMinutes: Int?,
        date: LocalDate,
        repeat: TaskRepeat,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) {
        viewModelScope.launch {
            repository.createTask(
                name = name,
                subtasks = subtasks,
                timerMinutes = timerMinutes,
                date = date.toEpochDay(),
                repeat = repeat,
                tag = tag,
                postponeIfIncomplete = postponeIfIncomplete
            )
        }
        selectDate(date)
    }

    fun updateTask(
        task: TaskEntity,
        name: String,
        subtasks: List<String>,
        timerMinutes: Int?,
        date: LocalDate,
        repeat: TaskRepeat,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) {
        viewModelScope.launch {
            repository.updateTask(
                task = task,
                name = name,
                subtasks = subtasks,
                timerMinutes = timerMinutes,
                date = date.toEpochDay(),
                repeat = repeat,
                tag = tag,
                postponeIfIncomplete = postponeIfIncomplete
            )
        }
        selectDate(date)
    }

    fun toggleTaskStatus(task: TaskEntity) {
        val newStatus = if (task.status == TaskStatus.COMPLETED) {
            TaskStatus.NOT_STARTED
        } else {
            TaskStatus.COMPLETED
        }
        viewModelScope.launch { repository.setStatus(task, newStatus) }
    }

    fun addCustomTag(name: String) {
        viewModelScope.launch { repository.addCustomTag(name) }
    }
}
