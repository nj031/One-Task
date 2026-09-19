package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.reminder.ReminderManager
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskOrderScope
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskRepository
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.data.task.SuccessCondition
import com.nj031.onetask.service.TimerForegroundService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    // Which month the Tasks calendar currently has open - drives datesWithTasksInCalendarMonth
    // below. Defaults to the current month so the very first query (before the calendar is even
    // opened) is still meaningful; the calendar itself always reopens on today's month regardless
    // of this value.
    private val _calendarVisibleMonth = MutableStateFlow(YearMonth.now())

    /** The set of dates within [_calendarVisibleMonth] that have at least one task - backs the
     * calendar's per-date task indicator dot. Reactive: any task change that adds/removes a date
     * from this set updates the dots automatically, the same way tasksForSelectedDate already
     * reacts to task changes. */
    val datesWithTasksInCalendarMonth: StateFlow<Set<LocalDate>> =
        _calendarVisibleMonth.flatMapLatest { month ->
            repository.observeDatesWithTasksBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
        }.map { epochDays -> epochDays.map(LocalDate::ofEpochDay).toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setCalendarVisibleMonth(month: YearMonth) {
        _calendarVisibleMonth.value = month
    }

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
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: LocalDate,
        priority: TaskPriority,
        reminderMinuteOfDay: Int?,
        reminderEpochDay: Long?,
        repeat: TaskRepeat,
        repeatDays: Set<DayOfWeek>,
        tag: String?,
        postponeIfIncomplete: Boolean,
        successCondition: SuccessCondition = SuccessCondition.ALL,
        successConditionThreshold: Int? = null
    ) {
        viewModelScope.launch {
            val created = repository.createTask(
                name = name,
                subtasks = subtasks,
                timerMinutes = timerMinutes,
                date = date.toEpochDay(),
                priority = priority,
                reminderMinuteOfDay = reminderMinuteOfDay,
                reminderEpochDay = reminderEpochDay,
                repeat = repeat,
                repeatDays = repeatDays,
                tag = tag,
                postponeIfIncomplete = postponeIfIncomplete,
                successCondition = successCondition,
                successConditionThreshold = successConditionThreshold
            )
            ReminderManager.reschedule(getApplication<Application>(), created)
        }
        selectDate(date)
    }

    fun updateTask(
        task: TaskEntity,
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: LocalDate,
        priority: TaskPriority,
        reminderMinuteOfDay: Int?,
        reminderEpochDay: Long?,
        repeat: TaskRepeat,
        repeatDays: Set<DayOfWeek>,
        tag: String?,
        postponeIfIncomplete: Boolean,
        successCondition: SuccessCondition = task.successCondition,
        successConditionThreshold: Int? = task.successConditionThreshold
    ) {
        viewModelScope.launch {
            val updated = repository.updateTask(
                task = task,
                name = name,
                subtasks = subtasks,
                timerMinutes = timerMinutes,
                date = date.toEpochDay(),
                priority = priority,
                reminderMinuteOfDay = reminderMinuteOfDay,
                reminderEpochDay = reminderEpochDay,
                repeat = repeat,
                repeatDays = repeatDays,
                tag = tag,
                postponeIfIncomplete = postponeIfIncomplete,
                successCondition = successCondition,
                successConditionThreshold = successConditionThreshold
            )
            ReminderManager.reschedule(getApplication<Application>(), updated)
        }
        selectDate(date)
    }

    /**
     * The checkbox's toggle. Completing reuses markTaskDone's pause-and-preserve logic (so
     * checking off a running timer isn't treated any differently from the action-sheet "Done"
     * button); un-completing restores a paused, resumable timer if the task had genuine
     * progress, or a plain Not Started state otherwise - never auto-starting either way.
     */
    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            val updated = if (task.status == TaskStatus.COMPLETED) {
                repository.uncompleteTask(task)
            } else {
                repository.markTaskDone(task)
            }
            // Completing today's occurrence must not cancel a recurring series' future
            // reminders, only advance today's own (see ReminderManager.reschedule) - and
            // un-completing a task whose one-shot reminder had already elapsed correctly leaves
            // it with nothing to reschedule.
            ReminderManager.reschedule(getApplication<Application>(), updated)
        }
    }

    fun addCustomTag(name: String) {
        viewModelScope.launch { repository.addCustomTag(name) }
    }

    fun deleteCustomTag(name: String) {
        viewModelScope.launch { repository.deleteCustomTag(name) }
    }

    fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            val updated = repository.toggleSubtask(task, subtaskId)
            ReminderManager.reschedule(getApplication<Application>(), updated)
        }
    }

    fun markTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            val updated = repository.markTaskDone(task)
            ReminderManager.reschedule(getApplication<Application>(), updated)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderManager.cancel(getApplication<Application>(), task.id)
        }
    }

    fun observeTask(id: String): Flow<TaskEntity?> = repository.observeTaskById(id)

    fun startTimer(task: TaskEntity) {
        viewModelScope.launch {
            // Must land in Room before the service starts observing this task's row, or the
            // service's very first read could still see the pre-start (not-running) state and
            // immediately stop itself before the real write ever arrives.
            repository.startTimer(task)
            // The foreground service is what keeps the countdown correct and notified while the
            // app is backgrounded or the screen is locked; it observes the task's Room row
            // itself, so this is a fire-and-forget kick-off, not a handle that needs to be told
            // to stop later.
            TimerForegroundService.start(getApplication<Application>(), task.id)
        }
    }

    fun pauseTimer(task: TaskEntity) {
        viewModelScope.launch { repository.pauseTimer(task) }
    }

    fun resetTimer(task: TaskEntity) {
        viewModelScope.launch { repository.resetTimer(task) }
    }

    fun finishTimer(task: TaskEntity) {
        viewModelScope.launch { repository.finishTimer(task) }
    }

    /** Persists a drag-and-drop reorder within a single tab - see [TaskRepository.reorderTasks]. */
    fun reorderTasks(scope: TaskOrderScope, orderedTasks: List<TaskEntity>) {
        viewModelScope.launch { repository.reorderTasks(scope, orderedTasks) }
    }
}
