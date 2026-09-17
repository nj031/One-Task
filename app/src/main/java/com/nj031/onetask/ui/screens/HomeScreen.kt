package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskAddButton
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskCalendarIcon
import com.nj031.onetask.ui.theme.OneTaskHamburgerIcon
import com.nj031.onetask.ui.theme.OneTaskTheme
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// Homepage-only palette (see design reference). Scoped to this file so Journal,
// Auth, and the Add Task sheet keep their existing theme colors untouched.
private val HomeBackground = Color(0xFFF4F7FC)
private val HomeCardWhite = Color(0xFFFFFFFF)
private val HomePrimaryBlue = Color(0xFF2F6FD6)
private val HomeDarkText = Color(0xFF17365D)
private val HomeSecondaryText = Color(0xFF6B7C93)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToJournal: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onOpenFocusTimer: (String) -> Unit = {},
    onAddTaskClick: () -> Unit = {},
    onEditTaskClick: (String) -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    onGeneralSettingsClick: () -> Unit = {},
    onDataPrivacyClick: () -> Unit = {},
    onUpgradeToProClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onHelpFeedbackClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY
) {
    // Which task's compact Action Row is currently open, if any - only one at a time, replacing
    // the old large tap-to-open action-sheet popup. Kept as an id (not the TaskEntity) so it
    // survives the underlying task object changing identity across recompositions/updates.
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val hapticTick = rememberHapticTick()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val activeTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
    val doneTasks = tasks.filter { it.status == TaskStatus.COMPLETED }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.6f),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                OneTaskDrawerContent(
                    userEmail = AuthRepository.currentUser?.email
                        ?: stringResource(id = R.string.sample_user_email),
                    onLogoutClick = onLogout,
                    onArchiveClick = {
                        scope.launch { drawerState.close() }
                        onArchiveClick()
                    },
                    onRecycleBinClick = {
                        scope.launch { drawerState.close() }
                        onRecycleBinClick()
                    },
                    onGeneralSettingsClick = {
                        scope.launch { drawerState.close() }
                        onGeneralSettingsClick()
                    },
                    onDataPrivacyClick = {
                        scope.launch { drawerState.close() }
                        onDataPrivacyClick()
                    },
                    onUpgradeToProClick = {
                        scope.launch { drawerState.close() }
                        onUpgradeToProClick()
                    },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        onAboutClick()
                    },
                    onHelpFeedbackClick = {
                        scope.launch { drawerState.close() }
                        onHelpFeedbackClick()
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = HomeBackground,
            floatingActionButton = {
                OneTaskAddButton(
                    onClick = onAddTaskClick,
                    contentDescription = stringResource(id = R.string.add_task)
                )
            },
            bottomBar = {
                OneTaskBottomNav(
                    activeTab = BottomNavTab.TASKS,
                    onJournalClick = onNavigateToJournal,
                    onTasksClick = {},
                    onProfileClick = onNavigateToProfile
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Tapping anywhere in this content area that isn't itself a clickable
                    // element (a Task Card, an Action Row button, a top-bar icon, ...) closes
                    // the open Action Row - those nested elements consume their own taps first,
                    // so this only ever fires for genuinely empty space. No dimming/overlay is
                    // added; this is a plain background tap, not a modal.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selectedTaskId = null },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    HomeTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCalendarClick = { showDatePicker = true }
                    )

                    DateNavigationRow(
                        selectedDate = selectedDate,
                        onPreviousDay = viewModel::goToPreviousDay,
                        onNextDay = viewModel::goToNextDay,
                        modifier = Modifier.padding(top = 24.dp)
                    )

                    val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
                    Text(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.tasks_completed, completedCount, tasks.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = HomePrimaryBlue,
                        textAlign = TextAlign.Center
                    )

                    if (tasks.isEmpty()) {
                        HomeEmptyState(modifier = Modifier.padding(top = 40.dp))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (activeTasks.isNotEmpty()) {
                                item(key = "section_in_progress") {
                                    SectionHeader(text = stringResource(id = R.string.status_in_progress))
                                }
                                items(activeTasks, key = { it.id }) { task ->
                                    HomeTaskListItem(
                                        task = task,
                                        selectedTaskId = selectedTaskId,
                                        onSelect = { selectedTaskId = task.id },
                                        onDeselect = { selectedTaskId = null },
                                        viewModel = viewModel,
                                        onOpenFocusTimer = onOpenFocusTimer,
                                        onEditTaskClick = onEditTaskClick,
                                        onDeleteConfirmRequired = { deleteConfirmTask = it }
                                    )
                                }
                            }
                            if (doneTasks.isNotEmpty()) {
                                item(key = "section_done") {
                                    SectionHeader(
                                        text = stringResource(id = R.string.section_done),
                                        modifier = Modifier.padding(top = if (activeTasks.isNotEmpty()) 8.dp else 0.dp)
                                    )
                                }
                                items(doneTasks, key = { it.id }) { task ->
                                    HomeTaskListItem(
                                        task = task,
                                        selectedTaskId = selectedTaskId,
                                        onSelect = { selectedTaskId = task.id },
                                        onDeselect = { selectedTaskId = null },
                                        viewModel = viewModel,
                                        onOpenFocusTimer = onOpenFocusTimer,
                                        onEditTaskClick = onEditTaskClick,
                                        onDeleteConfirmRequired = { deleteConfirmTask = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        OneTaskCalendarSheet(
            initialDate = selectedDate,
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false },
            weekStartDay = weekStartDay
        )
    }

    deleteConfirmTask?.let { task ->
        DeleteRunningTimerConfirmationSheet(
            onDelete = {
                hapticTick()
                viewModel.deleteTask(task)
                deleteConfirmTask = null
            },
            onCancel = { deleteConfirmTask = null }
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = HomePrimaryBlue,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun HomeTopBar(onMenuClick: () -> Unit, onCalendarClick: () -> Unit) {
    val menuDescription = stringResource(id = R.string.menu)
    val calendarDescription = stringResource(id = R.string.calendar)

    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .semantics { contentDescription = menuDescription }
        ) {
            OneTaskHamburgerIcon(tint = HomePrimaryBlue)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.home_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HomePrimaryBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.home_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = HomeSecondaryText,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .semantics { contentDescription = calendarDescription }
        ) {
            OneTaskCalendarIcon(tint = HomePrimaryBlue)
        }
    }
}

@Composable
private fun DateNavigationRow(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val relativeLabel = when (selectedDate) {
        today -> stringResource(id = R.string.today)
        today.minusDays(1) -> stringResource(id = R.string.yesterday)
        today.plusDays(1) -> stringResource(id = R.string.date_tomorrow)
        else -> selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.previous_day),
                tint = HomePrimaryBlue
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = relativeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomeDarkText
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = HomeSecondaryText
            )
        }

        IconButton(onClick = onNextDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.next_day),
                tint = HomePrimaryBlue
            )
        }
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HomeDarkText,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = HomeSecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Wires a single task's row of callbacks (select/deselect, timer Start/Continue/Reset, Undo,
 * Edit, and Delete-with-running-timer-confirmation) into [TaskCardWithActionRow]. Pulled out so
 * both the active-tasks and done-tasks sections of the list share identical wiring, the same way
 * onToggleStatus/onToggleSubtask were already shared between them before this redesign.
 */
@Composable
private fun HomeTaskListItem(
    task: TaskEntity,
    selectedTaskId: String?,
    onSelect: () -> Unit,
    onDeselect: () -> Unit,
    viewModel: HomeViewModel,
    onOpenFocusTimer: (String) -> Unit,
    onEditTaskClick: (String) -> Unit,
    onDeleteConfirmRequired: (TaskEntity) -> Unit
) {
    TaskCardWithActionRow(
        task = task,
        isSelected = task.id == selectedTaskId,
        onToggleStatus = { viewModel.toggleTaskStatus(task) },
        onClick = onSelect,
        onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) },
        onStart = {
            onDeselect()
            onOpenFocusTimer(task.id)
        },
        onContinue = {
            onDeselect()
            onOpenFocusTimer(task.id)
        },
        onReset = {
            viewModel.resetTimer(task)
            onDeselect()
        },
        onUndo = {
            viewModel.toggleTaskStatus(task)
            onDeselect()
        },
        onEdit = {
            onDeselect()
            onEditTaskClick(task.id)
        },
        onDelete = {
            onDeselect()
            // A task with a currently-running timer gets an extra confirmation step, since
            // deleting it also silently ends the active focus session - everything else deletes
            // immediately, matching existing behavior.
            if (task.timerEndAtMillis != null) {
                onDeleteConfirmRequired(task)
            } else {
                viewModel.deleteTask(task)
            }
        }
    )
}

/**
 * A Task Card plus its compact Action Row, shown directly above the card only while [isSelected]
 * - i.e. while this is the one task the user tapped. Keeping both in a single LazyColumn item
 * (rather than a separate item positioned before it) means only this task's own slot grows when
 * its Action Row opens; the rest of the list isn't rearranged, and nothing here is a popup,
 * bottom sheet, or dimmed overlay - it's plain content in the normal scroll flow.
 */
@Composable
private fun TaskCardWithActionRow(
    task: TaskEntity,
    isSelected: Boolean,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isSelected) {
            TaskActionRow(
                task = task,
                onStart = onStart,
                onContinue = onContinue,
                onReset = onReset,
                onUndo = onUndo,
                onEdit = onEdit,
                onDelete = onDelete,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        TaskCard(
            task = task,
            onToggleStatus = onToggleStatus,
            onClick = onClick,
            onToggleSubtask = onToggleSubtask
        )
    }
}

/**
 * The compact row of contextual actions for the selected task - Start/Continue/Reset/Undo/Edit/
 * Delete depending on [task]'s current timer/completion state, per the state table below. Never
 * includes a Done action: the circular checkbox on the card itself remains the one dedicated
 * completion control.
 *
 * | Timer? | Status      | Actions shown                |
 * |--------|-------------|-------------------------------|
 * | no     | not started | Edit, Delete                  |
 * | no     | completed   | Undo, Edit, Delete             |
 * | yes    | not started | Start, Edit, Delete            |
 * | yes    | in progress | Continue, Reset, Edit, Delete  |
 * | yes    | completed   | Undo, Edit, Delete             |
 */
@Composable
private fun TaskActionRow(
    task: TaskEntity,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val hasTimer = task.timerMinutes != null
    val isInProgress = task.status == TaskStatus.IN_PROGRESS

    val undoText = stringResource(id = R.string.task_action_undo)
    val continueText = stringResource(id = R.string.task_action_continue)
    val resetText = stringResource(id = R.string.reset)
    val startText = stringResource(id = R.string.task_action_start)
    val editText = stringResource(id = R.string.edit)
    val deleteText = stringResource(id = R.string.delete)

    val actions = buildList {
        when {
            isCompleted -> add(TaskRowAction(undoText, onUndo))
            hasTimer && isInProgress -> {
                add(TaskRowAction(continueText, onContinue))
                add(TaskRowAction(resetText, onReset))
            }
            hasTimer -> add(TaskRowAction(startText, onStart))
        }
        add(TaskRowAction(editText, onEdit))
        add(TaskRowAction(deleteText, onDelete, destructive = true))
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HomeCardWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEachIndexed { index, action ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .width(1.dp)
                                .height(16.dp)
                                .background(HomeSecondaryText.copy(alpha = 0.3f))
                        )
                    }
                    TaskRowActionButton(action = action)
                }
            }
        }
    }
}

private data class TaskRowAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false
)

@Composable
private fun TaskRowActionButton(action: TaskRowAction) {
    Text(
        text = action.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (action.destructive) MaterialTheme.colorScheme.error else HomePrimaryBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = action.onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit,
    onToggleSubtask: (String) -> Unit
) {
    var subtasksExpanded by remember(task.id) { mutableStateOf(false) }
    val isCompleted = task.status == TaskStatus.COMPLETED
    val subtasksInteractive = task.timerMinutes == null

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularTaskCheckbox(
                    checked = isCompleted,
                    onToggle = onToggleStatus
                )
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) HomeSecondaryText else HomeDarkText,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                task.tag?.let { tag ->
                    TagPill(text = tag, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        id = when (task.status) {
                            TaskStatus.NOT_STARTED -> R.string.status_not_started
                            TaskStatus.IN_PROGRESS -> R.string.status_in_progress
                            TaskStatus.COMPLETED -> R.string.status_completed
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = HomeSecondaryText
                )

                task.timerMinutes?.let { minutes ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.timer_minutes_format, minutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = HomeSecondaryText
                    )
                }

                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.subtasks_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = HomePrimaryBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { subtasksExpanded = !subtasksExpanded }
                    )
                }
            }

            if (subtasksExpanded && task.subtasks.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    task.subtasks.forEach { subtask ->
                        SubtaskRow(
                            subtask = subtask,
                            interactive = subtasksInteractive,
                            onToggle = { onToggleSubtask(subtask.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, HomePrimaryBlue, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = HomePrimaryBlue
        )
    }
}

@Composable
private fun SubtaskRow(
    subtask: Subtask,
    interactive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.5.dp, HomePrimaryBlue, CircleShape)
                .then(if (interactive) Modifier.clickable(onClick = onToggle) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (subtask.completed) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HomePrimaryBlue
                )
            }
        }
        Text(
            text = subtask.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.completed) HomeSecondaryText else HomeDarkText,
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun CircularTaskCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticTick = rememberHapticTick()
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, HomePrimaryBlue, CircleShape)
            .clickable(onClick = {
                hapticTick()
                onToggle()
            }),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HomePrimaryBlue
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteRunningTimerConfirmationSheet(
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = HomeCardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.delete_task_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomePrimaryBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.delete_task_confirm_message),
                style = MaterialTheme.typography.bodySmall,
                color = HomeSecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            FilledTonalButton(
                onClick = { dismissThen(onDelete) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(id = R.string.delete), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(
                onClick = { dismissThen(onCancel) },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeSecondaryText
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    OneTaskTheme {
        HomeScreen()
    }
}
