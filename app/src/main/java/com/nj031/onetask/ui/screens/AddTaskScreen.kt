package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.repeatDaysSet
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private const val TIMER_25_MIN = 25
private const val TIMER_45_MIN = 45
private const val TIMER_60_MIN = 60
private val TIMER_PRESETS = listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN)

/**
 * The dedicated full-screen "New Task" / "Edit Task" page opened from the general Add (+)
 * button or from editing an existing task. Resolves [taskId] against the shared [viewModel]
 * (the same instance Home uses) so editing sees live data, and falls straight through for a
 * brand-new task (taskId == null).
 */
@Composable
fun AddTaskScreen(
    viewModel: HomeViewModel,
    taskId: String?,
    defaultTimerMinutes: Int? = null,
    defaultTag: String? = null,
    defaultPostponeIfIncomplete: Boolean = true,
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
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
    val customTags by viewModel.customTags.collectAsState()

    // Wait for an edit-mode lookup to resolve before rendering the form, rather than seeding
    // fields with blank defaults and swapping them for the real values a frame later.
    if (taskId == null || existingTask != null) {
        AddTaskScreenContent(
            existingTask = existingTask,
            initialDate = existingTask?.date?.let(LocalDate::ofEpochDay) ?: homeSelectedDate,
            // Default Task Settings only ever seed a brand-new task's initial fields - an
            // existing task being edited always keeps showing its own saved values, since
            // existingTask?.x is already non-null in that case and short-circuits the default.
            initialTimerMinutes = existingTask?.timerMinutes ?: defaultTimerMinutes,
            initialTag = existingTask?.tag ?: defaultTag,
            initialPostponeIfIncomplete = existingTask?.postponeIfIncomplete ?: defaultPostponeIfIncomplete,
            weekStartDay = weekStartDay,
            customTags = customTags,
            onAddCustomTag = { name -> viewModel.addCustomTag(name) },
            onCancel = onDone,
            onSave = { name, subtasks, timerMinutes, date, priority, repeat, repeatDays, tag, postpone ->
                if (existingTask != null) {
                    viewModel.updateTask(
                        task = existingTask,
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        priority = priority,
                        repeat = repeat,
                        repeatDays = repeatDays,
                        tag = tag,
                        postponeIfIncomplete = postpone
                    )
                } else {
                    viewModel.createTask(
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        priority = priority,
                        repeat = repeat,
                        repeatDays = repeatDays,
                        tag = tag,
                        postponeIfIncomplete = postpone
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
    existingTask: TaskEntity?,
    initialDate: LocalDate,
    initialTimerMinutes: Int?,
    initialTag: String?,
    initialPostponeIfIncomplete: Boolean,
    weekStartDay: DayOfWeek,
    customTags: List<String>,
    onAddCustomTag: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: LocalDate,
        priority: TaskPriority,
        repeat: TaskRepeat,
        repeatDays: Set<DayOfWeek>,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticTick = rememberHapticTick()

    var taskName by remember { mutableStateOf(existingTask?.name.orEmpty()) }
    val taskNameFocusRequester = remember { FocusRequester() }

    val subtasks = remember {
        mutableStateListOf<Subtask>().apply { addAll(existingTask?.subtasks ?: emptyList()) }
    }
    val subtaskFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var pendingFocusSubtaskId by remember { mutableStateOf<String?>(null) }

    var selectedTaskDate by remember { mutableStateOf(initialDate) }
    var showDatePickerSheet by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf(existingTask?.priority ?: TaskPriority.NONE) }

    var selectedRepeat by remember { mutableStateOf(existingTask?.repeat ?: TaskRepeat.NONE) }
    val selectedRepeatDays = remember {
        mutableStateListOf<DayOfWeek>().apply { addAll(existingTask?.repeatDaysSet().orEmpty()) }
    }
    var selectedTag by remember { mutableStateOf(initialTag) }
    var showAddTagSheet by remember { mutableStateOf(false) }
    val defaultTagNames = listOf(
        stringResource(id = R.string.tag_personal),
        stringResource(id = R.string.tag_study),
        stringResource(id = R.string.tag_health),
        stringResource(id = R.string.tag_work)
    )

    // Pending Task and Repeat are mutually exclusive: a recurring task's occurrences are already
    // independent per-date, so "postpone to today if incomplete" (which only makes sense for a
    // single one-time task) is force-disabled whenever Repeat isn't "Does not repeat" - both for
    // an existing recurring task being edited (hence the selectedRepeat check here too, not just
    // in the effect below) and the instant the user picks Daily/Select Days on a new one.
    var postponeIfIncomplete by remember {
        mutableStateOf(initialPostponeIfIncomplete && selectedRepeat == TaskRepeat.NONE)
    }
    LaunchedEffect(selectedRepeat) {
        if (selectedRepeat != TaskRepeat.NONE) {
            postponeIfIncomplete = false
        }
    }

    var timerMinutes by remember { mutableStateOf(initialTimerMinutes) }
    val isInitialCustomTimer = initialTimerMinutes != null && initialTimerMinutes !in TIMER_PRESETS
    var showCustomTimerInput by remember { mutableStateOf(isInitialCustomTimer) }
    var customTimerText by remember {
        mutableStateOf(if (isInitialCustomTimer) initialTimerMinutes.toString() else "")
    }
    val customTimerFocusRequester = remember { FocusRequester() }

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

    LaunchedEffect(showCustomTimerInput) {
        if (showCustomTimerInput) {
            customTimerFocusRequester.requestFocus()
            keyboardController?.show()
        }
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                AddTaskTopBar(isEditMode = existingTask != null, onBackClick = onCancel)

                RowLabel(text = stringResource(id = R.string.task_name_label), topPadding = 28.dp)
                TextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .focusRequester(taskNameFocusRequester),
                    placeholder = {
                        Text(text = stringResource(id = R.string.task_name_placeholder))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = taskFieldColors()
                )

                SettingRow(
                    label = stringResource(id = R.string.tag_label),
                    modifier = Modifier.padding(top = 26.dp)
                ) {
                    Column {
                        TagGroupLabel(text = stringResource(id = R.string.default_tags_group_label))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            defaultTagNames.forEach { tag ->
                                SelectionChip(
                                    text = tag,
                                    selected = tag == selectedTag,
                                    onClick = { selectedTag = if (selectedTag == tag) null else tag }
                                )
                            }
                        }
                        TagGroupLabel(
                            text = stringResource(id = R.string.custom_tags_section),
                            topPadding = 14.dp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customTags.forEach { tag ->
                                SelectionChip(
                                    text = tag,
                                    selected = tag == selectedTag,
                                    onClick = { selectedTag = if (selectedTag == tag) null else tag }
                                )
                            }
                            SelectionChip(
                                text = stringResource(id = R.string.option_custom),
                                selected = false,
                                onClick = { showAddTagSheet = true }
                            )
                        }
                    }
                }

                SettingRow(
                    label = stringResource(id = R.string.priority_label),
                    modifier = Modifier.padding(top = 26.dp)
                ) {
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

                SettingRow(
                    label = stringResource(id = R.string.date_label),
                    modifier = Modifier.padding(top = 26.dp)
                ) {
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
                        .padding(top = 6.dp, start = 92.dp)
                )

                SettingRow(
                    label = stringResource(id = R.string.repeat_label),
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    Column {
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
                        if (selectedRepeat == TaskRepeat.SELECT_DAYS) {
                            FlowRow(
                                modifier = Modifier.padding(top = 8.dp),
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
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                SettingRow(
                    label = stringResource(id = R.string.timer_label),
                    modifier = Modifier.padding(top = 26.dp)
                ) {
                    Column {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectionChip(
                                text = stringResource(id = R.string.option_no_timer),
                                selected = !showCustomTimerInput && timerMinutes == null,
                                onClick = {
                                    timerMinutes = null
                                    showCustomTimerInput = false
                                }
                            )
                            listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN).forEach { minutes ->
                                SelectionChip(
                                    text = minutes.toString(),
                                    selected = !showCustomTimerInput && timerMinutes == minutes,
                                    onClick = {
                                        timerMinutes = minutes
                                        showCustomTimerInput = false
                                    }
                                )
                            }
                            SelectionChip(
                                text = stringResource(id = R.string.option_custom),
                                selected = showCustomTimerInput,
                                onClick = { showCustomTimerInput = true }
                            )
                        }
                        if (showCustomTimerInput) {
                            TextField(
                                value = customTimerText,
                                onValueChange = { customTimerText = it.filter(Char::isDigit) },
                                placeholder = {
                                    Text(stringResource(id = R.string.custom_timer_minutes_hint))
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = taskFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .focusRequester(customTimerFocusRequester)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.subtasks_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
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
                            .padding(top = 8.dp),
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(id = R.string.postpone_if_incomplete),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(id = R.string.pending_task_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = postponeIfIncomplete,
                        enabled = selectedRepeat == TaskRepeat.NONE,
                        onCheckedChange = {
                            hapticTick()
                            postponeIfIncomplete = it
                        }
                    )
                }

                Button(
                    onClick = {
                        onSave(
                            taskName.trim(),
                            subtasks.map { it.copy(name = it.name.trim()) }
                                .filter { it.name.isNotBlank() },
                            if (showCustomTimerInput) customTimerText.toIntOrNull() else timerMinutes,
                            selectedTaskDate,
                            selectedPriority,
                            selectedRepeat,
                            selectedRepeatDays.toSet(),
                            selectedTag,
                            postponeIfIncomplete
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .height(56.dp),
                    enabled = taskName.isNotBlank() &&
                        (selectedRepeat != TaskRepeat.SELECT_DAYS || selectedRepeatDays.isNotEmpty()),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = stringResource(
                            id = if (existingTask != null) R.string.save else R.string.create_task_button
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePickerSheet) {
        OneTaskCalendarSheet(
            initialDate = selectedTaskDate,
            onDateSelected = { selectedTaskDate = it },
            onDismiss = { showDatePickerSheet = false },
            weekStartDay = weekStartDay
        )
    }

    if (showAddTagSheet) {
        AddTagSheet(
            existingTags = defaultTagNames + customTags,
            onAdd = { name ->
                onAddCustomTag(name)
                selectedTag = name
                showAddTagSheet = false
            },
            onDismiss = { showAddTagSheet = false }
        )
    }
}

@Composable
private fun AddTaskTopBar(isEditMode: Boolean, onBackClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(modifier = Modifier.padding(top = 14.dp, start = 4.dp)) {
            Text(
                text = stringResource(
                    id = if (isEditMode) R.string.edit_task_title else R.string.new_task_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.new_task_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Label-left, options-right structure shared by the Tag/Date/Repeat/Timer rows. */
@Composable
private fun SettingRow(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .width(84.dp)
                .padding(top = 10.dp, end = 8.dp)
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun RowLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = topPadding)
    )
}

/** Small header above the Default Tags / Custom Tags chip groups in the Tag row. */
@Composable
private fun TagGroupLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding, bottom = 6.dp)
    )
}

/** A pill-shaped selectable option used by the Tag/Date/Repeat/Timer rows - light-blue fill
 * with bold primary-colored text when selected, plain white otherwise, no borders. */
@Composable
private fun SelectionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
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
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** A small light-blue pill button for an inline "add" action (e.g. "+ Add Subtask"). */
@Composable
private fun AddChipButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
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

@Composable
private fun taskFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTagSheet(existingTags: List<String>, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var tagName by remember { mutableStateOf("") }
    val trimmedName = tagName.trim()
    val isDuplicate = trimmedName.isNotEmpty() &&
        existingTags.any { it.equals(trimmedName, ignoreCase = true) }

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(id = R.string.add_tag),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                placeholder = { Text(stringResource(id = R.string.tag_label)) },
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    {
                        Text(
                            text = stringResource(id = R.string.custom_tag_error_duplicate),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = taskFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { dismissThen { onAdd(trimmedName) } },
                enabled = trimmedName.isNotEmpty() && !isDuplicate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(id = R.string.date_picker_ok), fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { dismissThen(onDismiss) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
