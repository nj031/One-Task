package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.TimeFormat
import com.nj031.onetask.data.task.CategoryEntity
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.SuccessCondition
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.ui.components.CategorySelectorDialog
import com.nj031.onetask.ui.components.OneTaskCalendarDialog
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.components.OneTaskTimePickerDialog
import com.nj031.onetask.ui.components.categoryDisplayName
import com.nj031.onetask.ui.components.durationMillisToWholeMinutes
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.viewmodel.AddTaskDraftViewModel
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val TIMER_25_MIN = 25
private const val TIMER_45_MIN = 45
private const val TIMER_60_MIN = 60
private val TIMER_PRESETS = listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN)

// This screen's own timerMinutes field only ever stores whole minutes (see TaskEntity.timerMinutes),
// so the shared H/M/S OneTaskDurationPickerDialog's confirm button is disabled below a full minute
// - a "custom" duration below that would otherwise round down to the same as "No Timer" (0/null).
private const val MIN_CUSTOM_TASK_TIMER_MILLIS = 60_000L

/**
 * The dedicated full-screen "New Task" / "Edit Task" page opened from the general Add (+)
 * button or from editing an existing task. Resolves [taskId] against the shared [viewModel]
 * (the same instance Home uses) so editing sees live data, and falls straight through for a
 * brand-new task (taskId == null).
 */
@Composable
fun AddTaskScreen(
    viewModel: HomeViewModel,
    draftViewModel: AddTaskDraftViewModel,
    taskId: String?,
    customCategories: List<CategoryEntity> = emptyList(),
    onAddCategoryClick: () -> Unit = {},
    defaultTimerMinutes: Int? = null,
    defaultTag: String? = null,
    defaultPostponeIfIncomplete: Boolean = true,
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    onDone: () -> Unit
) {
    val existingTaskState = produceState<TaskEntity?>(initialValue = null, key1 = taskId) {
        value = null
        if (taskId != null) {
            viewModel.observeTask(taskId).collect { value = it }
        }
    }
    val existingTask = existingTaskState.value
    val homeSelectedDate by viewModel.selectedDate.collectAsState()

    // Wait for an edit-mode lookup to resolve before rendering the form, rather than seeding
    // fields with blank defaults and swapping them for the real values a frame later.
    if (taskId == null || existingTask != null) {
        // See AddTaskDraftViewModel's own doc comment - a no-op if this is a resume (after a
        // round trip to Settings' Custom Category management) of the same draft, so it never
        // clobbers what the user already entered.
        draftViewModel.initializeIfNeeded(
            key = taskId ?: "new",
            existingTask = existingTask,
            initialDate = existingTask?.date?.let(LocalDate::ofEpochDay) ?: homeSelectedDate,
            // Default Task Settings only ever seed a brand-new task's initial fields - an
            // existing task being edited always keeps showing its own saved values, since
            // existingTask?.x is already non-null in that case and short-circuits the default.
            initialTimerMinutes = defaultTimerMinutes,
            initialPostponeIfIncomplete = defaultPostponeIfIncomplete
        )
        AddTaskScreenContent(
            draft = draftViewModel,
            existingTask = existingTask,
            initialTag = existingTask?.tag ?: defaultTag,
            customCategories = customCategories,
            onAddCategoryClick = onAddCategoryClick,
            weekStartDay = weekStartDay,
            timeFormat = timeFormat,
            onCancel = onDone,
            onSave = {
                name, subtasks, timerMinutes, date, priority, reminderMinuteOfDay, reminderEpochDay,
                repeat, repeatDays, tag, categoryId, postpone, successCondition, successConditionThreshold ->
                if (existingTask != null) {
                    viewModel.updateTask(
                        task = existingTask,
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        priority = priority,
                        reminderMinuteOfDay = reminderMinuteOfDay,
                        reminderEpochDay = reminderEpochDay,
                        repeat = repeat,
                        repeatDays = repeatDays,
                        tag = tag,
                        categoryId = categoryId,
                        postponeIfIncomplete = postpone,
                        successCondition = successCondition,
                        successConditionThreshold = successConditionThreshold
                    )
                } else {
                    viewModel.createTask(
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        priority = priority,
                        reminderMinuteOfDay = reminderMinuteOfDay,
                        reminderEpochDay = reminderEpochDay,
                        repeat = repeat,
                        repeatDays = repeatDays,
                        tag = tag,
                        categoryId = categoryId,
                        postponeIfIncomplete = postpone,
                        successCondition = successCondition,
                        successConditionThreshold = successConditionThreshold
                    )
                }
                onDone()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskScreenContent(
    draft: AddTaskDraftViewModel,
    existingTask: TaskEntity?,
    initialTag: String?,
    customCategories: List<CategoryEntity>,
    onAddCategoryClick: () -> Unit,
    weekStartDay: DayOfWeek,
    timeFormat: TimeFormat,
    onCancel: () -> Unit,
    onSave: (
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
        categoryId: String?,
        postponeIfIncomplete: Boolean,
        successCondition: SuccessCondition,
        successConditionThreshold: Int?
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticTick = rememberHapticTick()

    var taskName by remember(draft) { mutableStateOf(draft.taskName) }
    // Mirrors every write back into the hoisted draft so it's what actually survives a round
    // trip to Settings - see AddTaskDraftViewModel's own doc comment. Kept as a local `taskName`
    // (rather than reading/writing draft.taskName directly at every use site below) purely so
    // this screen's existing structure - every other field the same way - doesn't need touching.
    LaunchedEffect(taskName) { draft.taskName = taskName }
    val taskNameFocusRequester = remember { FocusRequester() }

    var selectedPriority by remember(draft) { mutableStateOf(draft.selectedPriority) }
    LaunchedEffect(selectedPriority) { draft.selectedPriority = selectedPriority }

    val subtasks = draft.subtasks
    val subtaskFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var pendingFocusSubtaskId by remember { mutableStateOf<String?>(null) }

    // Success Condition (see SuccessCondition/TaskEntity.effectiveSuccessConditionThreshold):
    // ALL is the same sensible default a task without an explicit choice already gets.
    // successConditionThreshold only matters once CUSTOM is picked - it's kept clamped into
    // 1..subtasks.size below whenever subtasks shrink, but is never bumped up on its own when
    // subtasks grow, exactly like the persisted entity's own clamping behaves.
    var selectedSuccessCondition by remember(draft) { mutableStateOf(draft.selectedSuccessCondition) }
    LaunchedEffect(selectedSuccessCondition) { draft.selectedSuccessCondition = selectedSuccessCondition }
    var successConditionThreshold by remember(draft) { mutableStateOf(draft.successConditionThreshold) }
    LaunchedEffect(successConditionThreshold) { draft.successConditionThreshold = successConditionThreshold }
    LaunchedEffect(subtasks.size) {
        val threshold = successConditionThreshold
        if (threshold != null && subtasks.isNotEmpty() && threshold > subtasks.size) {
            successConditionThreshold = subtasks.size
        }
    }
    val successConditionLocked = subtasks.isEmpty()

    var selectedTaskDate by remember(draft) { mutableStateOf(draft.selectedTaskDate) }
    LaunchedEffect(selectedTaskDate) { draft.selectedTaskDate = selectedTaskDate }
    var showDatePickerSheet by remember { mutableStateOf(false) }

    var selectedRepeat by remember(draft) { mutableStateOf(draft.selectedRepeat) }
    LaunchedEffect(selectedRepeat) { draft.selectedRepeat = selectedRepeat }
    val selectedRepeatDays = draft.selectedRepeatDays

    // Reminder: at most one per task. reminderDate always holds a concrete date (defaulting to
    // today, exactly like selectedTaskDate above) rather than being null while "Custom Date" is
    // selected but not yet picked - the same "Custom" chip is considered selected whenever the
    // resolved date isn't today/tomorrow" pattern the Date row above already uses, reused here
    // rather than inventing a second convention. reminderEnabled off is the literal "No Reminder"
    // default state.
    var reminderEnabled by remember(draft) { mutableStateOf(draft.reminderEnabled) }
    LaunchedEffect(reminderEnabled) { draft.reminderEnabled = reminderEnabled }
    var reminderDate by remember(draft) { mutableStateOf(draft.reminderDate) }
    LaunchedEffect(reminderDate) { draft.reminderDate = reminderDate }
    var reminderMinuteOfDay by remember(draft) { mutableStateOf(draft.reminderMinuteOfDay) }
    LaunchedEffect(reminderMinuteOfDay) { draft.reminderMinuteOfDay = reminderMinuteOfDay }
    var showReminderDatePickerSheet by remember { mutableStateOf(false) }
    var showReminderTimeSheet by remember { mutableStateOf(false) }

    val reminderDateTime = remember(reminderDate, reminderMinuteOfDay) {
        reminderMinuteOfDay?.let { minute -> LocalDateTime.of(reminderDate, LocalTime.of(minute / 60, minute % 60)) }
    }
    val reminderMissingTime = reminderEnabled && reminderMinuteOfDay == null
    // Only meaningful for a plain (non-recurring) reminder - a Daily/Select Days reminder always
    // fires on some future applicable day regardless of which date happened to be selected here
    // (see ReminderScheduler), so the past-time guard only blocks Save for the one case where the
    // selected date+time is literally what will be used to fire.
    val reminderTimeIsInPast = reminderEnabled && selectedRepeat == TaskRepeat.NONE &&
        reminderDateTime != null && !reminderDateTime.isAfter(LocalDateTime.now())
    val reminderIsInvalid = reminderMissingTime || reminderTimeIsInPast

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Denied or granted - either way, saving/scheduling proceeds identically; see
           ReminderReceiver's own SecurityException guard for what an actual denial means at
           notification-delivery time. */ }
    LaunchedEffect(reminderEnabled) {
        if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The Tag row's own picker UI has been replaced on this screen by the Category section below
    // - task.tag itself is untouched data, so whatever value this task already had (or Default
    // Task Settings seeds a new one with) simply keeps flowing straight through to onSave
    // unedited.
    val tag = initialTag

    var categoryId by remember(draft) { mutableStateOf(draft.categoryId) }
    LaunchedEffect(categoryId) { draft.categoryId = categoryId }
    var showCategorySelector by remember { mutableStateOf(false) }

    // Pending Task and Repeat are mutually exclusive: a recurring task's occurrences are already
    // independent per-date, so "postpone to today if incomplete" (which only makes sense for a
    // single one-time task) is force-disabled whenever Repeat isn't "Does not repeat" - both for
    // an existing recurring task being edited (hence the selectedRepeat check here too, not just
    // in the effect below) and the instant the user picks Daily/Select Days on a new one.
    var postponeIfIncomplete by remember(draft) { mutableStateOf(draft.postponeIfIncomplete) }
    LaunchedEffect(postponeIfIncomplete) { draft.postponeIfIncomplete = postponeIfIncomplete }
    LaunchedEffect(selectedRepeat) {
        if (selectedRepeat != TaskRepeat.NONE) {
            postponeIfIncomplete = false
        }
    }

    var timerMinutes by remember(draft) { mutableStateOf(draft.timerMinutes) }
    LaunchedEffect(timerMinutes) { draft.timerMinutes = timerMinutes }
    var showCustomDurationPicker by remember { mutableStateOf(false) }

    val formattedTaskDate = remember(selectedTaskDate) {
        selectedTaskDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

    BackHandler(onBack = onCancel)

    LaunchedEffect(Unit) {
        taskNameFocusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(pendingFocusSubtaskId) {
        val id = pendingFocusSubtaskId ?: return@LaunchedEffect
        subtaskFocusRequesters[id]?.requestFocus()
        keyboardController?.show()
        pendingFocusSubtaskId = null
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AddTaskTopBar(isEditMode = existingTask != null, onBackClick = onCancel)

                Spacer(modifier = Modifier.height(6.dp))

                // 1. Task Name
                TaskSectionCard {
                    Text(
                        text = stringResource(id = R.string.task_name_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .focusRequester(taskNameFocusRequester),
                        placeholder = {
                            Text(text = stringResource(id = R.string.task_name_placeholder))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = taskFieldColors()
                    )
                }

                // 2. Subtasks
                TaskSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.subtasks_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        AddChipButton(
                            text = stringResource(id = R.string.add_subtask),
                            onClick = {
                                val newSubtask = Subtask(name = "")
                                subtasks.add(newSubtask)
                                pendingFocusSubtaskId = newSubtask.id
                            }
                        )
                    }
                    subtasks.forEachIndexed { index, subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = subtask.name,
                                onValueChange = { subtasks[index] = subtask.copy(name = it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(
                                        subtaskFocusRequesters.getOrPut(subtask.id) { FocusRequester() }
                                    ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = taskFieldColors()
                            )
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .clickable(onClickLabel = stringResource(id = R.string.remove)) {
                                        subtasks.removeAt(index)
                                    }
                            )
                        }
                    }
                }

                // 3. Success Condition
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.success_condition_label)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.success_condition_all),
                                selected = selectedSuccessCondition == SuccessCondition.ALL,
                                enabled = !successConditionLocked,
                                onClick = { selectedSuccessCondition = SuccessCondition.ALL }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.success_condition_any_one),
                                selected = selectedSuccessCondition == SuccessCondition.ANY_ONE,
                                enabled = !successConditionLocked,
                                onClick = { selectedSuccessCondition = SuccessCondition.ANY_ONE }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.success_condition_custom),
                                selected = selectedSuccessCondition == SuccessCondition.CUSTOM,
                                enabled = !successConditionLocked,
                                onClick = {
                                    selectedSuccessCondition = SuccessCondition.CUSTOM
                                    if (successConditionThreshold == null) {
                                        successConditionThreshold = subtasks.size
                                    }
                                }
                            )
                        }
                    }
                    if (successConditionLocked) {
                        Text(
                            text = stringResource(id = R.string.success_condition_no_subtasks_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (selectedSuccessCondition == SuccessCondition.CUSTOM) {
                        FlowRow(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val effectiveThreshold = successConditionThreshold ?: subtasks.size
                            (1..subtasks.size).forEach { count ->
                                SelectionChip(
                                    text = count.toString(),
                                    selected = effectiveThreshold == count,
                                    onClick = { successConditionThreshold = count }
                                )
                            }
                        }
                    }
                }

                // 4. Timer
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.timer_label)) {
                        val isCustomTimerSelected = timerMinutes != null && timerMinutes !in TIMER_PRESETS
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.option_no_timer),
                                selected = !isCustomTimerSelected && timerMinutes == null,
                                onClick = { timerMinutes = null }
                            )
                            listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN).forEach { minutes ->
                                SelectionChip(
                                    text = minutes.toString(),
                                    selected = !isCustomTimerSelected && timerMinutes == minutes,
                                    onClick = { timerMinutes = minutes }
                                )
                            }
                            SelectionChip(
                                text = stringResource(id = R.string.option_custom),
                                selected = isCustomTimerSelected,
                                onClick = { showCustomDurationPicker = true }
                            )
                        }
                    }
                }

                // 5. Category - tapping the name label itself does nothing (it's a static
                // display, not a control); only "Choose Category" opens the selector. Creating a
                // new category is never done inline here - the selector's own "+ Add Category"
                // hands off to Settings' Custom Category management instead (see
                // onAddCategoryClick).
                TaskSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryId?.let { categoryDisplayName(it, customCategories) }
                                ?: stringResource(id = R.string.category_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        AddChipButton(
                            text = stringResource(id = R.string.choose_category_button),
                            onClick = { showCategorySelector = true }
                        )
                    }
                }

                // 6. Date
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.date_label)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.today),
                                selected = selectedTaskDate == today,
                                onClick = { selectedTaskDate = today }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.date_tomorrow),
                                selected = selectedTaskDate == today.plusDays(1),
                                onClick = { selectedTaskDate = today.plusDays(1) }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.option_custom),
                                selected = selectedTaskDate != today && selectedTaskDate != today.plusDays(1),
                                onClick = { showDatePickerSheet = true }
                            )
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.task_will_be_added_to, formattedTaskDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                // 7. Repeat
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.repeat_label)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.repeat_none),
                                selected = selectedRepeat == TaskRepeat.NONE,
                                onClick = { selectedRepeat = TaskRepeat.NONE }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.repeat_daily),
                                selected = selectedRepeat == TaskRepeat.DAILY,
                                onClick = { selectedRepeat = TaskRepeat.DAILY }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.repeat_select_days),
                                selected = selectedRepeat == TaskRepeat.SELECT_DAYS,
                                onClick = { selectedRepeat = TaskRepeat.SELECT_DAYS }
                            )
                        }
                    }
                    if (selectedRepeat == TaskRepeat.SELECT_DAYS) {
                        FlowRow(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOfWeek.values().forEach { day ->
                                SelectionChip(
                                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    selected = day in selectedRepeatDays,
                                    onClick = {
                                        if (day in selectedRepeatDays) {
                                            selectedRepeatDays.remove(day)
                                        } else {
                                            selectedRepeatDays.add(day)
                                        }
                                    }
                                )
                            }
                        }
                        if (selectedRepeatDays.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.repeat_select_days_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // 8. Reminder
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.reminder_label)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.reminder_none),
                                selected = !reminderEnabled,
                                onClick = { reminderEnabled = false }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.today),
                                selected = reminderEnabled && reminderDate == today,
                                onClick = {
                                    reminderEnabled = true
                                    reminderDate = today
                                }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.date_tomorrow),
                                selected = reminderEnabled && reminderDate == today.plusDays(1),
                                onClick = {
                                    reminderEnabled = true
                                    reminderDate = today.plusDays(1)
                                }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.reminder_custom_date),
                                selected = reminderEnabled &&
                                    reminderDate != today && reminderDate != today.plusDays(1),
                                onClick = {
                                    reminderEnabled = true
                                    showReminderDatePickerSheet = true
                                }
                            )
                        }
                    }
                    if (reminderEnabled) {
                        val is24Hour = timeFormat.resolveIs24Hour(context)
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionChip(
                                text = reminderMinuteOfDay?.let { formatReminderTime(it, is24Hour) }
                                    ?: stringResource(id = R.string.reminder_select_time),
                                selected = reminderMinuteOfDay != null,
                                onClick = { showReminderTimeSheet = true }
                            )
                        }
                        if (reminderIsInvalid) {
                            Text(
                                text = stringResource(id = R.string.reminder_time_in_past_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // 9. Priority
                TaskSectionCard {
                    CardRow(label = stringResource(id = R.string.priority_label)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.priority_small),
                                selected = selectedPriority == TaskPriority.SMALL,
                                onClick = {
                                    selectedPriority =
                                        if (selectedPriority == TaskPriority.SMALL) TaskPriority.NONE else TaskPriority.SMALL
                                }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.priority_medium),
                                selected = selectedPriority == TaskPriority.MEDIUM,
                                onClick = {
                                    selectedPriority =
                                        if (selectedPriority == TaskPriority.MEDIUM) TaskPriority.NONE else TaskPriority.MEDIUM
                                }
                            )
                            SelectionChip(
                                text = stringResource(id = R.string.priority_high),
                                selected = selectedPriority == TaskPriority.HIGH,
                                onClick = {
                                    selectedPriority =
                                        if (selectedPriority == TaskPriority.HIGH) TaskPriority.NONE else TaskPriority.HIGH
                                }
                            )
                        }
                    }
                }

                // 10. Pending Task
                TaskSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.postpone_if_incomplete),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = postponeIfIncomplete,
                            enabled = selectedRepeat == TaskRepeat.NONE,
                            onCheckedChange = {
                                hapticTick()
                                postponeIfIncomplete = it
                            }
                        )
                    }
                }

                // 11. Bottom action button
                Button(
                    onClick = {
                        val finalSubtasks = subtasks.map { it.copy(name = it.name.trim()) }
                            .filter { it.name.isNotBlank() }
                        val finalSuccessConditionThreshold =
                            if (selectedSuccessCondition == SuccessCondition.CUSTOM && finalSubtasks.isNotEmpty()) {
                                (successConditionThreshold ?: finalSubtasks.size)
                                    .coerceIn(1, finalSubtasks.size)
                            } else {
                                null
                            }
                        onSave(
                            taskName.trim(),
                            finalSubtasks,
                            timerMinutes,
                            selectedTaskDate,
                            selectedPriority,
                            if (reminderEnabled) reminderMinuteOfDay else null,
                            if (reminderEnabled) reminderDate.toEpochDay() else null,
                            selectedRepeat,
                            selectedRepeatDays.toSet(),
                            tag,
                            categoryId,
                            postponeIfIncomplete,
                            selectedSuccessCondition,
                            finalSuccessConditionThreshold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(56.dp),
                    enabled = taskName.isNotBlank() &&
                        (selectedRepeat != TaskRepeat.SELECT_DAYS || selectedRepeatDays.isNotEmpty()) &&
                        !reminderIsInvalid,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = stringResource(
                            id = if (existingTask != null) R.string.save_changes_button else R.string.add_task_button
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showDatePickerSheet) {
        OneTaskCalendarDialog(
            selectedDate = selectedTaskDate,
            onDateSelected = { selectedTaskDate = it },
            onDismiss = { showDatePickerSheet = false },
            weekStartDay = weekStartDay
        )
    }

    if (showReminderDatePickerSheet) {
        OneTaskCalendarDialog(
            selectedDate = reminderDate,
            onDateSelected = { reminderDate = it },
            onDismiss = { showReminderDatePickerSheet = false },
            minSelectableDate = today,
            weekStartDay = weekStartDay
        )
    }

    if (showReminderTimeSheet) {
        OneTaskTimePickerDialog(
            initialMinuteOfDay = reminderMinuteOfDay,
            onTimeSelected = {
                reminderMinuteOfDay = it
                showReminderTimeSheet = false
            },
            onDismiss = { showReminderTimeSheet = false }
        )
    }

    if (showCustomDurationPicker) {
        OneTaskDurationPickerDialog(
            initialMillis = (timerMinutes ?: 0).toLong() * 60_000L,
            minDurationMillis = MIN_CUSTOM_TASK_TIMER_MILLIS,
            onDismiss = { showCustomDurationPicker = false },
            onConfirm = { millis ->
                timerMinutes = durationMillisToWholeMinutes(millis)
                showCustomDurationPicker = false
            }
        )
    }

    if (showCategorySelector) {
        CategorySelectorDialog(
            currentCategoryId = categoryId,
            customCategories = customCategories,
            onDismiss = { showCategorySelector = false },
            onConfirm = { chosen ->
                categoryId = chosen
                showCategorySelector = false
            },
            onAddCategoryClick = {
                showCategorySelector = false
                onAddCategoryClick()
            }
        )
    }
}

/** SYSTEM_DEFAULT defers to the device's own 12/24-hour setting, matching every other place in
 * Android that displays a time picker when the user hasn't explicitly overridden it in this
 * app's own General Settings > Time Format. */
private fun TimeFormat.resolveIs24Hour(context: Context): Boolean = when (this) {
    TimeFormat.HOUR_24 -> true
    TimeFormat.HOUR_12 -> false
    TimeFormat.SYSTEM_DEFAULT -> android.text.format.DateFormat.is24HourFormat(context)
}

private fun formatReminderTime(minuteOfDay: Int, is24Hour: Boolean): String {
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/** Back button + centered title, matching the target design's plain centered header (no
 * subtitle). */
@Composable
private fun AddTaskTopBar(isEditMode: Boolean, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = if (isEditMode) R.string.edit_task_title else R.string.new_task_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/** The white, rounded-corner card every Add/Edit Task section is presented in - the container
 * that gives this screen its card-based look, matching the target design. */
@Composable
private fun TaskSectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content
    )
}

/** Label-left, content-right structure used inside a [TaskSectionCard] for a section whose
 * label and chip row/control fit on one line (Success Condition/Timer/Date/Repeat/Reminder/
 * Priority). The label's width is intrinsic (not fixed), matching how each row's content starts
 * right after its own label in the target design. */
@Composable
private fun CardRow(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp, end = 12.dp)
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/** A pill-shaped selectable option used inside a [TaskSectionCard]'s chip rows - filled with the
 * theme's secondary-container tint and bold primary-colored text when selected, an outlined
 * white pill otherwise. [enabled] false renders it visibly inert (dimmed, unclickable) - used for
 * a locked Success Condition (no subtasks yet). */
@Composable
private fun SelectionChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        },
        shape = RoundedCornerShape(50),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** A small pill button filled with the theme's secondary-container tint, for an inline "add"
 * action (e.g. "+ Add Subtask") or the inert "+ Choose Category" placeholder. [enabled] false
 * keeps the exact same look but makes it unclickable - used for the Category placeholder, which
 * must not implement any selection behavior yet. */
@Composable
private fun AddChipButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/** Input field fill distinct from both the white [TaskSectionCard] behind it and the green
 * selected-chip tint - uses the theme's own background tone (see Theme.kt) so it stays correct
 * under every Appearance color, not just the reference design's green. */
@Composable
private fun taskFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.background,
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent
)
