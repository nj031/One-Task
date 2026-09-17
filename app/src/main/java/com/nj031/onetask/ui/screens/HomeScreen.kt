package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskOrderScope
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskAddIcon
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
    // All is the default per spec - every task for the day is visible until the user narrows
    // it down, matching what this screen always showed before tabs existed.
    var selectedTab by remember { mutableStateOf(TaskOrderScope.ALL) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val hapticTick = rememberHapticTick()
    val listState = rememberLazyListState()

    // Drag-and-drop reorder state, scoped to whichever tab is currently on screen. draggedTaskId
    // is non-null only while a long-press-drag is in progress; dragOffsetY is that one task's
    // live, cumulative finger movement in px, applied as a visual translation. displayedTasks is
    // the on-screen order - normally just [visibleTasks], but during a drag it's the optimistic,
    // already-swapped order so cards visibly shift before the reorder is persisted.
    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var displayedTasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    // A view/filter over the same task list, sorted by the current tab's own independent manual
    // order - switching tabs, or completing a task, never reorders or touches another tab's
    // order.
    val visibleTasks = when (selectedTab) {
        TaskOrderScope.ALL -> tasks.sortedBy { it.orderInAll }
        TaskOrderScope.IN_PROGRESS ->
            tasks.filter { it.status == TaskStatus.IN_PROGRESS }.sortedBy { it.orderInProgress }
        TaskOrderScope.DONE ->
            tasks.filter { it.status == TaskStatus.COMPLETED }.sortedBy { it.orderInDone }
    }

    // Only re-sync from the real (persisted) order while nothing is actively being dragged, so a
    // fresh Flow emission mid-drag can't yank the list back to the pre-drag order under the
    // user's finger.
    LaunchedEffect(visibleTasks, draggedTaskId) {
        if (draggedTaskId == null) {
            displayedTasks = visibleTasks
        }
    }

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

                    HomeAddTaskBar(
                        onClick = onAddTaskClick,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    HomeTaskTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    if (visibleTasks.isEmpty()) {
                        HomeEmptyState(modifier = Modifier.padding(top = 40.dp))
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedTasks, key = { it.id }) { task ->
                                val isDragged = task.id == draggedTaskId
                                HomeTaskListItem(
                                    task = task,
                                    selectedTaskId = selectedTaskId,
                                    onSelect = {
                                        selectedTaskId = if (selectedTaskId == task.id) null else task.id
                                    },
                                    onDeselect = { selectedTaskId = null },
                                    viewModel = viewModel,
                                    onOpenFocusTimer = onOpenFocusTimer,
                                    onEditTaskClick = onEditTaskClick,
                                    onDeleteConfirmRequired = { deleteConfirmTask = it },
                                    isDragged = isDragged,
                                    dragOffsetY = if (isDragged) dragOffsetY else 0f,
                                    onDragStart = {
                                        // A drag always wins over an open Action Row - opening
                                        // one is a short-tap-only action (see HomeTaskListItem's
                                        // onSelect toggle) and dragging is a completely separate
                                        // long-press interaction, so nothing else about tapping
                                        // this or any other card changes.
                                        selectedTaskId = null
                                        draggedTaskId = task.id
                                        dragOffsetY = 0f
                                        hapticTick()
                                    },
                                    onDrag = { deltaY ->
                                        dragOffsetY += deltaY
                                        val (reordered, correctedOffset) = dragSwapIfNeeded(
                                            tasks = displayedTasks,
                                            draggedTaskId = task.id,
                                            dragOffsetY = dragOffsetY,
                                            listState = listState
                                        )
                                        displayedTasks = reordered
                                        dragOffsetY = correctedOffset
                                    },
                                    onDragEnd = {
                                        val finalOrder = displayedTasks
                                        draggedTaskId = null
                                        dragOffsetY = 0f
                                        viewModel.reorderTasks(selectedTab, finalOrder)
                                    },
                                    modifier = if (isDragged) Modifier else Modifier.animateItem()
                                )
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

/**
 * The Tasks screen's Add Task control - a horizontal rounded bar in the normal page flow,
 * replacing the old circular floating action button. It's a general/global control (not scoped
 * to any one tab below it) and triggers the exact same [onClick] the FAB used to call; only its
 * shape and position changed. Uses the same HomePrimaryBlue/white pairing the old FAB used
 * (OneTaskAddButton's own AddButtonBlue is this same color), so it stays visually consistent
 * with the rest of this screen's existing palette.
 */
@Composable
private fun HomeAddTaskBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val addTaskDescription = stringResource(id = R.string.add_task)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(HomePrimaryBlue)
            .clickable(onClick = onClick)
            .semantics { contentDescription = addTaskDescription }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneTaskAddIcon(size = 20.dp, drawContainer = false, plusColor = Color.White)
        Text(
            text = addTaskDescription,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Replaces the old In Progress/Done section headings with a 3-way filter over the same task
 * list. IN_PROGRESS and DONE reuse [TaskStatus] exactly as it already worked before this change
 * (status == IN_PROGRESS covers a running, paused, or finished-but-not-completed timer; status ==
 * COMPLETED only ever changes via the task's own checkbox) - no new completion/timer logic was
 * introduced, this is purely a presentation change. Reuses [TaskOrderScope] (rather than a
 * separate UI-only enum) since each tab is now also a distinct drag-and-drop order scope - one
 * ALL/IN_PROGRESS/DONE concept, not two.
 */
@Composable
private fun HomeTaskTabRow(
    selectedTab: TaskOrderScope,
    onTabSelected: (TaskOrderScope) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TaskOrderScope.ALL to stringResource(id = R.string.tab_all),
        TaskOrderScope.IN_PROGRESS to stringResource(id = R.string.status_in_progress),
        TaskOrderScope.DONE to stringResource(id = R.string.section_done)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeCardWhite)
            .padding(4.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = tab == selectedTab
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else HomeSecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(if (isSelected) Modifier.background(HomePrimaryBlue) else Modifier)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp)
            )
        }
    }
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
 * Swaps the dragged task with whichever immediate neighbor (in [tasks]'s current order) its live
 * drag position has now crossed the center of, using [listState]'s own real, per-item measured
 * heights - so this works correctly even though Task Cards aren't all the same height (a longer
 * name, a tag pill, or expanded subtasks). Returns the possibly-reordered list alongside a
 * corrected [dragOffsetY]: since the dragged item's own laid-out slot position jumps by exactly
 * the swapped neighbor's height the instant the swap happens, subtracting/adding that same
 * amount keeps the card's apparent on-screen position continuous under the user's finger rather
 * than visibly snapping at the moment of the swap. A no-op (returns the inputs unchanged) once
 * this frame's crossing doesn't warrant a swap, or if layout info for the relevant items isn't
 * available yet (e.g. scrolled just out of view).
 */
private fun dragSwapIfNeeded(
    tasks: List<TaskEntity>,
    draggedTaskId: String,
    dragOffsetY: Float,
    listState: LazyListState
): Pair<List<TaskEntity>, Float> {
    val draggedIndex = tasks.indexOfFirst { it.id == draggedTaskId }
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val draggedInfo = visibleItems.find { it.key == draggedTaskId }
    if (draggedIndex < 0 || draggedInfo == null) return tasks to dragOffsetY
    val draggedCenter = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY

    val nextInfo = tasks.getOrNull(draggedIndex + 1)?.let { next ->
        visibleItems.find { it.key == next.id }
    }
    if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size / 2f) {
        val reordered = tasks.toMutableList().apply { add(draggedIndex + 1, removeAt(draggedIndex)) }
        return reordered to (dragOffsetY - nextInfo.size)
    }

    val prevInfo = tasks.getOrNull(draggedIndex - 1)?.let { prev ->
        visibleItems.find { it.key == prev.id }
    }
    if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size / 2f) {
        val reordered = tasks.toMutableList().apply { add(draggedIndex - 1, removeAt(draggedIndex)) }
        return reordered to (dragOffsetY + prevInfo.size)
    }

    return tasks to dragOffsetY
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
    onDeleteConfirmRequired: (TaskEntity) -> Unit,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
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
        },
        isDragged = isDragged,
        dragOffsetY = dragOffsetY,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        modifier = modifier
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
    onDelete: () -> Unit,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
            onToggleSubtask = onToggleSubtask,
            isDragged = isDragged,
            dragOffsetY = dragOffsetY,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd
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
    onToggleSubtask: (String) -> Unit,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var subtasksExpanded by remember(task.id) { mutableStateOf(false) }
    val isCompleted = task.status == TaskStatus.COMPLETED
    val subtasksInteractive = task.timerMinutes == null

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer { translationY = dragOffsetY }
            // A drag-to-reorder gesture that only activates after Android's own standard
            // long-press timeout - not a custom one - so a normal short tap keeps opening the
            // Action Row exactly as before (see onClick above) and is never mistaken for the
            // start of a drag.
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCardWhite),
        // Only a very slight elevation bump communicates "this card is now draggable" - no
        // scale, rotation, or size change.
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 4.dp else 1.dp)
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
