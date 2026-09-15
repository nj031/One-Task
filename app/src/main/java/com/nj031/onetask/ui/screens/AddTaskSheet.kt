package com.nj031.onetask.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val TIMER_25_MIN = 25
private const val TIMER_45_MIN = 45
private const val TIMER_60_MIN = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    viewModel: HomeViewModel,
    initialDate: LocalDate,
    existingTask: TaskEntity?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val customTags by viewModel.customTags.collectAsState()

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddTaskSheetContent(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            initialDate = initialDate,
            existingTask = existingTask,
            customTags = customTags,
            onCancel = ::dismiss,
            onAddCustomTag = { name -> viewModel.addCustomTag(name) },
            onSave = { name, subtasks, timerMinutes, date, repeat, tag, postpone ->
                if (existingTask != null) {
                    viewModel.updateTask(
                        task = existingTask,
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        repeat = repeat,
                        tag = tag,
                        postponeIfIncomplete = postpone
                    )
                } else {
                    viewModel.createTask(
                        name = name,
                        subtasks = subtasks,
                        timerMinutes = timerMinutes,
                        date = date,
                        repeat = repeat,
                        tag = tag,
                        postponeIfIncomplete = postpone
                    )
                }
                dismiss()
            }
        )
    }
}

@Composable
private fun AddTaskSheetContent(
    modifier: Modifier = Modifier,
    initialDate: LocalDate,
    existingTask: TaskEntity?,
    customTags: List<String>,
    onCancel: () -> Unit,
    onAddCustomTag: (String) -> Unit,
    onSave: (
        name: String,
        subtasks: List<Subtask>,
        timerMinutes: Int?,
        date: LocalDate,
        repeat: TaskRepeat,
        tag: String?,
        postponeIfIncomplete: Boolean
    ) -> Unit
) {
    val today = remember { LocalDate.now() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var taskName by remember { mutableStateOf(existingTask?.name.orEmpty()) }
    val taskNameFocusRequester = remember { FocusRequester() }
    val subtasks = remember {
        mutableStateListOf<Subtask>().apply { addAll(existingTask?.subtasks ?: emptyList()) }
    }
    val subtaskFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var pendingFocusSubtaskId by remember { mutableStateOf<String?>(null) }
    var timerMinutes by remember { mutableStateOf(existingTask?.timerMinutes) }
    var selectedTaskDate by remember {
        mutableStateOf(existingTask?.date?.let(LocalDate::ofEpochDay) ?: initialDate)
    }
    var selectedRepeat by remember { mutableStateOf(existingTask?.repeat ?: TaskRepeat.NONE) }
    var selectedTag by remember { mutableStateOf(existingTask?.tag) }
    var postponeIfIncomplete by remember {
        mutableStateOf(existingTask?.postponeIfIncomplete ?: true)
    }

    var showTimerSheet by remember { mutableStateOf(false) }
    var showDatePickerSheet by remember { mutableStateOf(false) }
    var showAddTagSheet by remember { mutableStateOf(false) }

    val formattedTaskDate = remember(selectedTaskDate) {
        selectedTaskDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                id = if (existingTask != null) R.string.save else R.string.add_task_title
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        SectionLabel(text = stringResource(id = R.string.task_name_label))
        TextField(
            value = taskName,
            onValueChange = { taskName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .focusRequester(taskNameFocusRequester),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        SectionLabel(text = stringResource(id = R.string.subtasks_label), topPadding = 14.dp)
        subtasks.forEachIndexed { index, subtask ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
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
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
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
        CompactActionButton(
            text = stringResource(id = R.string.add_subtask),
            onClick = {
                val newSubtask = Subtask(name = "")
                subtasks.add(newSubtask)
                pendingFocusSubtaskId = newSubtask.id
            },
            modifier = Modifier.padding(top = 6.dp)
        )

        SectionLabel(text = stringResource(id = R.string.timer_label), topPadding = 14.dp)
        if (timerMinutes == null) {
            CompactActionButton(
                text = stringResource(id = R.string.add_timer),
                onClick = { showTimerSheet = true },
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactActionButton(
                    text = stringResource(id = R.string.timer_minutes_format, timerMinutes ?: 0),
                    onClick = { showTimerSheet = true },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(onClickLabel = stringResource(id = R.string.remove)) {
                            timerMinutes = null
                        }
                )
            }
        }

        SectionLabel(text = stringResource(id = R.string.date_label), topPadding = 14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.today),
                selected = selectedTaskDate == today,
                onClick = { selectedTaskDate = today },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.date_tomorrow),
                selected = selectedTaskDate == today.plusDays(1),
                onClick = { selectedTaskDate = today.plusDays(1) },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.date_choose),
                selected = selectedTaskDate != today && selectedTaskDate != today.plusDays(1),
                onClick = { showDatePickerSheet = true },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(id = R.string.task_will_be_added_to, formattedTaskDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            textAlign = TextAlign.Center
        )

        SectionLabel(text = stringResource(id = R.string.repeat_label), topPadding = 14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.repeat_none),
                selected = selectedRepeat == TaskRepeat.NONE,
                onClick = { selectedRepeat = TaskRepeat.NONE },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.repeat_daily),
                selected = selectedRepeat == TaskRepeat.DAILY,
                onClick = { selectedRepeat = TaskRepeat.DAILY },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.repeat_weekly),
                selected = selectedRepeat == TaskRepeat.WEEKLY,
                onClick = { selectedRepeat = TaskRepeat.WEEKLY },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.repeat_monthly),
                selected = selectedRepeat == TaskRepeat.MONTHLY,
                onClick = { selectedRepeat = TaskRepeat.MONTHLY },
                modifier = Modifier.weight(1f)
            )
        }

        SectionLabel(text = stringResource(id = R.string.tag_label), topPadding = 14.dp)
        TagsRow(
            customTags = customTags,
            selectedTag = selectedTag,
            onToggleTag = { tag ->
                selectedTag = if (selectedTag == tag) null else tag
            },
            onAddTagClick = { showAddTagSheet = true },
            modifier = Modifier.padding(top = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.postpone_if_incomplete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = postponeIfIncomplete,
                onCheckedChange = { postponeIfIncomplete = it }
            )
        }

        CompactActionButton(
            text = stringResource(
                id = if (existingTask != null) R.string.save else R.string.add_task_title
            ),
            onClick = {
                onSave(
                    taskName.trim(),
                    subtasks.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotBlank() },
                    timerMinutes,
                    selectedTaskDate,
                    selectedRepeat,
                    selectedTag,
                    postponeIfIncomplete
                )
            },
            modifier = Modifier.padding(top = 18.dp),
            enabled = taskName.isNotBlank(),
            emphasized = true
        )

        TextButton(
            onClick = onCancel,
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

    if (showTimerSheet) {
        TimerPickerSheet(
            onSelect = { minutes ->
                timerMinutes = minutes
                showTimerSheet = false
            },
            onDismiss = { showTimerSheet = false }
        )
    }

    if (showDatePickerSheet) {
        OneTaskCalendarSheet(
            initialDate = selectedTaskDate,
            onDateSelected = { selectedTaskDate = it },
            onDismiss = { showDatePickerSheet = false }
        )
    }

    if (showAddTagSheet) {
        AddTagSheet(
            onAdd = { name ->
                onAddCustomTag(name)
                selectedTag = name
                showAddTagSheet = false
            },
            onDismiss = { showAddTagSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerPickerSheet(onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }
    val customMinutes = customMinutesText.toIntOrNull()
    val customMinutesFocusRequester = remember { FocusRequester() }

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    LaunchedEffect(showCustomInput) {
        if (showCustomInput) {
            customMinutesFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.study_timer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            if (!showCustomInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN).forEach { minutes ->
                        CompactActionButton(
                            text = stringResource(id = R.string.timer_minutes_format, minutes),
                            onClick = { dismissThen { onSelect(minutes) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CompactActionButton(
                    text = stringResource(id = R.string.repeat_custom),
                    onClick = { showCustomInput = true },
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                TextField(
                    value = customMinutesText,
                    onValueChange = { customMinutesText = it.filter(Char::isDigit) },
                    placeholder = { Text(stringResource(id = R.string.custom_timer_minutes_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(customMinutesFocusRequester)
                )
                CompactActionButton(
                    text = stringResource(id = R.string.set_timer),
                    onClick = { customMinutes?.let { minutes -> dismissThen { onSelect(minutes) } } },
                    enabled = customMinutes != null && customMinutes > 0,
                    emphasized = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            TextButton(
                onClick = { dismissThen(onDismiss) },
                modifier = Modifier.padding(top = 4.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTagSheet(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var tagName by remember { mutableStateOf("") }

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
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            CompactActionButton(
                text = stringResource(id = R.string.date_picker_ok),
                onClick = { dismissThen { onAdd(tagName.trim()) } },
                enabled = tagName.isNotBlank(),
                emphasized = true,
                modifier = Modifier.padding(top = 10.dp)
            )
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

/** A compact tonal button used only within these panels - shorter than the app's shared button. */
@Composable
private fun CompactActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding)
    )
}

@Composable
private fun RowScope.SelectionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(
    customTags: List<String>,
    selectedTag: String?,
    onToggleTag: (String) -> Unit,
    onAddTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultTags = listOf(
        stringResource(id = R.string.tag_personal),
        stringResource(id = R.string.tag_work),
        stringResource(id = R.string.tag_study),
        stringResource(id = R.string.tag_health)
    )
    val allTags = remember(defaultTags, customTags) {
        (defaultTags + customTags).distinct()
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allTags.forEach { tag ->
            val selected = tag == selectedTag
            FilterChip(
                selected = selected,
                onClick = { onToggleTag(tag) },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(50),
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        AssistChip(
            onClick = onAddTagClick,
            label = {
                Text(
                    text = stringResource(id = R.string.add_tag),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            shape = RoundedCornerShape(50),
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}
