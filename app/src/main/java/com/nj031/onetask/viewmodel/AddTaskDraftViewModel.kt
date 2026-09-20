package com.nj031.onetask.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.SuccessCondition
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.repeatDaysSet
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The in-progress Add/Edit Task form, hoisted above AddTaskScreen's own composable - created
 * once in OneTaskNavHost, exactly like HomeViewModel - so it survives a round trip to Settings
 * and back (see the Category section's "+ Add Category" flow, which sends the user to Settings'
 * Custom Category management and back to this same screen). A plain `remember` inside
 * AddTaskScreenContent would be disposed the moment Navigation Compose swaps that destination
 * out of composition, which is exactly what happens while Settings is on screen; a ViewModel
 * scoped this high in the graph is not.
 *
 * [initializeIfNeeded] seeds every field from either a blank task or [TaskEntity] being edited -
 * but only the first time this draft is used for a given [draftKey], never on a later
 * recomposition for the same key, so resuming after a Settings round trip keeps whatever the
 * user had already entered instead of reloading from the (possibly now-stale) existingTask
 * snapshot. [clear] resets [draftKey] to null and must be called by the caller whenever the user
 * actually leaves the screen (Save or Cancel/Back) - see AddTaskScreen's onDone wiring in
 * OneTaskNavHost - so the next genuinely new Add Task never resumes a stale, already-finished
 * draft.
 */
class AddTaskDraftViewModel : ViewModel() {
    var draftKey: String? = null
        private set

    var taskName by mutableStateOf("")
    var selectedPriority by mutableStateOf(TaskPriority.NONE)
    val subtasks = mutableStateListOf<Subtask>()
    var selectedSuccessCondition by mutableStateOf(SuccessCondition.ALL)
    var successConditionThreshold by mutableStateOf<Int?>(null)
    var selectedTaskDate by mutableStateOf(LocalDate.now())
    var selectedRepeat by mutableStateOf(TaskRepeat.NONE)
    val selectedRepeatDays = mutableStateListOf<DayOfWeek>()
    var reminderEnabled by mutableStateOf(false)
    var reminderDate by mutableStateOf(LocalDate.now())
    var reminderMinuteOfDay by mutableStateOf<Int?>(null)
    var postponeIfIncomplete by mutableStateOf(true)
    var timerMinutes by mutableStateOf<Int?>(null)
    var categoryId by mutableStateOf<String?>(null)

    fun initializeIfNeeded(
        key: String,
        existingTask: TaskEntity?,
        initialDate: LocalDate,
        initialTimerMinutes: Int?,
        initialPostponeIfIncomplete: Boolean
    ) {
        if (draftKey == key) return
        draftKey = key
        taskName = existingTask?.name.orEmpty()
        selectedPriority = existingTask?.priority ?: TaskPriority.NONE
        subtasks.clear()
        subtasks.addAll(existingTask?.subtasks ?: emptyList())
        selectedSuccessCondition = existingTask?.successCondition ?: SuccessCondition.ALL
        successConditionThreshold = existingTask?.successConditionThreshold
        selectedTaskDate = initialDate
        selectedRepeat = existingTask?.repeat ?: TaskRepeat.NONE
        selectedRepeatDays.clear()
        selectedRepeatDays.addAll(existingTask?.repeatDaysSet().orEmpty())
        reminderEnabled = existingTask?.reminderMinuteOfDay != null
        reminderDate = existingTask?.reminderEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        reminderMinuteOfDay = existingTask?.reminderMinuteOfDay
        postponeIfIncomplete =
            initialPostponeIfIncomplete && (existingTask?.repeat ?: TaskRepeat.NONE) == TaskRepeat.NONE
        timerMinutes = existingTask?.timerMinutes ?: initialTimerMinutes
        categoryId = existingTask?.categoryId
    }

    /** Called whenever the user actually leaves the screen (Save or Cancel/Back) - see this
     * class's own doc comment for why a mid-flow trip to Settings must NOT call this. */
    fun clear() {
        draftKey = null
    }
}
