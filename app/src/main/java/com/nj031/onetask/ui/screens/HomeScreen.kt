package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.nj031.onetask.ui.theme.OneTaskCalendarIcon
import com.nj031.onetask.ui.theme.OneTaskHamburgerIcon
import com.nj031.onetask.ui.theme.OneTaskTheme
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// Homepage-only palette (see design reference). Scoped to this file so Journal,
// Auth, and the Add Task sheet keep their existing theme colors untouched.
private val HomeBackground = Color(0xFFF4F7FC)
private val HomeButtonLight = Color(0xFFDBEBFA)
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
    onDataPrivacyClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var actionMenuTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                    onGeneralSettingsClick = { /* no-op: not implemented yet */ },
                    onDataPrivacyClick = {
                        scope.launch { drawerState.close() }
                        onDataPrivacyClick()
                    },
                    onUpgradeToProClick = { /* no-op: Premium not available yet */ },
                    onAboutClick = { /* no-op: not implemented yet */ },
                    onHelpFeedbackClick = { /* no-op: help & feedback not implemented yet */ },
                    onRateAppClick = { /* no-op: not published yet */ }
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
                    .padding(innerPadding),
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
                                    TaskCard(
                                        task = task,
                                        onToggleStatus = { viewModel.toggleTaskStatus(task) },
                                        onClick = { actionMenuTask = task },
                                        onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) }
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
                                    TaskCard(
                                        task = task,
                                        onToggleStatus = { viewModel.toggleTaskStatus(task) },
                                        onClick = { actionMenuTask = task },
                                        onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) }
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
            onDismiss = { showDatePicker = false }
        )
    }

    actionMenuTask?.let { task ->
        TaskActionSheet(
            task = task,
            onStartOrContinueClick = {
                actionMenuTask = null
                onOpenFocusTimer(task.id)
            },
            onResetClick = {
                viewModel.resetTimer(task)
                actionMenuTask = null
            },
            onEditClick = {
                actionMenuTask = null
                onEditTaskClick(task.id)
            },
            onDoneClick = {
                viewModel.markTaskDone(task)
                actionMenuTask = null
            },
            onDeleteClick = {
                actionMenuTask = null
                // A task with a currently-running timer gets an extra confirmation step, since
                // deleting it also silently ends the active focus session - everything else
                // deletes immediately, matching existing behavior.
                if (task.timerEndAtMillis != null) {
                    deleteConfirmTask = task
                } else {
                    viewModel.deleteTask(task)
                }
            },
            onDismiss = { actionMenuTask = null }
        )
    }

    deleteConfirmTask?.let { task ->
        DeleteRunningTimerConfirmationSheet(
            onDelete = {
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
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) HomeSecondaryText else HomeDarkText,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                task.tag?.let { tag ->
                    TagPill(text = tag, modifier = Modifier.padding(start = 8.dp))
                }
                CircularTaskCheckbox(
                    checked = isCompleted,
                    onToggle = onToggleStatus,
                    modifier = Modifier.padding(start = 12.dp)
                )
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
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, HomePrimaryBlue, CircleShape)
            .clickable(onClick = onToggle),
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
private fun TaskActionSheet(
    task: TaskEntity,
    onStartOrContinueClick: () -> Unit,
    onResetClick: () -> Unit,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
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
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomePrimaryBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (task.timerMinutes != null) {
                val hasStarted = task.status != TaskStatus.NOT_STARTED
                TaskActionButton(
                    text = "▶  " + stringResource(
                        id = if (hasStarted) R.string.task_action_continue else R.string.task_action_start
                    ),
                    onClick = { dismissThen(onStartOrContinueClick) }
                )
                TaskActionButton(
                    text = "↻  " + stringResource(id = R.string.reset),
                    onClick = { dismissThen(onResetClick) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            TaskActionButton(
                text = "✎  " + stringResource(id = R.string.edit),
                onClick = { dismissThen(onEditClick) },
                modifier = Modifier.padding(top = 8.dp)
            )
            TaskActionButton(
                text = "✓  " + stringResource(id = R.string.task_action_done),
                onClick = { dismissThen(onDoneClick) },
                modifier = Modifier.padding(top = 8.dp)
            )
            TaskActionButton(
                text = "🗑  " + stringResource(id = R.string.delete),
                onClick = { dismissThen(onDeleteClick) },
                isDestructive = true,
                modifier = Modifier.padding(top = 8.dp)
            )

            TextButton(
                onClick = { dismissThen(onDismiss) },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    stringResource(id = R.string.cancel),
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeSecondaryText
                )
            }
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

@Composable
private fun TaskActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (isDestructive) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        } else {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = HomeButtonLight,
                contentColor = HomeDarkText
            )
        }
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    OneTaskTheme {
        HomeScreen()
    }
}
