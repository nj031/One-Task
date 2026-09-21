package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.data.task.CategoryEntity
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskOrderScope
import com.nj031.onetask.data.task.TaskPriority
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.CategorySelectorDialog
import com.nj031.onetask.ui.components.categoryDisplayName
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskAddButton
import com.nj031.onetask.ui.components.OneTaskCalendarDialog
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.ProfileAvatar
import com.nj031.onetask.ui.components.WallpaperBackdrop
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskCalendarIcon
import com.nj031.onetask.ui.theme.OneTaskTasksIcon
import com.nj031.onetask.ui.theme.OneTaskTheme
import com.nj031.onetask.ui.theme.OneTaskWallpapers
import com.nj031.onetask.viewmodel.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Drag-and-drop edge auto-scroll tuning - see the LaunchedEffect(draggedTaskId) auto-scroll
// effect in HomeScreen below. The zone is measured from the list's own viewport edge, not the
// screen edge, so it's unaffected by the app bar/tab row/bottom nav surrounding the list.
private const val AUTO_SCROLL_EDGE_ZONE_DP = 72
private const val AUTO_SCROLL_MIN_SPEED_DP_PER_SEC = 200f
private const val AUTO_SCROLL_MAX_SPEED_DP_PER_SEC = 1200f

// The task list's bottom content padding needs to clear the floating "+" button, which Scaffold
// positions above/independent of the list (its own height is never reflected in the Scaffold
// content lambda's innerPadding). FAB_SCAFFOLD_END_MARGIN_DP is Material3 Scaffold's own spacing
// between a FabPosition.End FAB and the surrounding edges/bottom bar - not a guess at this
// screen's layout, but the same constant Scaffold itself positions the FAB with. The FAB's own
// height is measured live (see fabHeight below) rather than hardcoded, so this stays correct even
// if the FAB's size ever changes. FAB_SAFETY_GAP_DP is the small extra breathing room so the last
// card never visually touches the button.
private const val FAB_SCAFFOLD_END_MARGIN_DP = 16
private const val FAB_SAFETY_GAP_DP = 8

/** The Tasks screen's compact filter strip (All/Basic/Timer/Category - see [TaskFilterStrip]).
 * BASIC/TIMER classification depends only on whether
 * [com.nj031.onetask.data.task.TaskEntity.timerMinutes] is set - never Reminder, Category,
 * Priority, Subtasks, Repeat, or Pending Task; this is unchanged by Category's own filter mode
 * below. All/Basic/Timer/Category are filtered views over the exact same manual order (see
 * [TaskOrderScope.ALL]'s use in HomeScreen), not independent drag-and-drop scopes the way the old
 * In Progress/Done tabs were.
 *
 * [CATEGORY.categoryId] is which category the filter is currently narrowed to - null means "No
 * Category" (a task with no category assigned), never "nothing chosen yet"; tapping the Category
 * pill always opens [CategorySelectorDialog] to choose or change it (see
 * onCategoryFilterClick below), it never toggles CATEGORY on by itself. */
private sealed class TaskListFilter {
    object ALL : TaskListFilter()
    object BASIC : TaskListFilter()
    object TIMER : TaskListFilter()
    data class CATEGORY(val categoryId: String?) : TaskListFilter()
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    profilePhotoPath: String? = null,
    onNavigateToJournal: () -> Unit = {},
    onOpenTimerPlaceholder: () -> Unit = {},
    onProfileAvatarClick: () -> Unit = {},
    onOpenFocusTimer: (String) -> Unit = {},
    onAddTaskClick: () -> Unit = {},
    onEditTaskClick: (String) -> Unit = {},
    onAddCategoryClick: () -> Unit = {},
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    wallpaper: Wallpaper = Wallpaper.NONE,
    darkTheme: Boolean = false
) {
    // Which task's compact Action Row is currently open, if any - only one at a time, replacing
    // the old large tap-to-open action-sheet popup. Kept as an id (not the TaskEntity) so it
    // survives the underlying task object changing identity across recompositions/updates.
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    // All is the default per spec - every task for the day is visible until the user narrows
    // it down.
    var selectedFilter by remember { mutableStateOf<TaskListFilter>(TaskListFilter.ALL) }
    var showCategoryFilterSelector by remember { mutableStateOf(false) }
    val customCategories by viewModel.customCategories.collectAsState()
    val hapticTick = rememberHapticTick()
    val listState = rememberLazyListState()
    // The FAB's real, measured height - see FAB_SCAFFOLD_END_MARGIN_DP's own comment for why this
    // (rather than a hardcoded size) drives the task list's bottom content padding.
    var fabHeight by remember { mutableStateOf(0.dp) }

    // Drag-and-drop reorder state, scoped to whichever tab is currently on screen. draggedTaskId
    // is non-null only while a long-press-drag is in progress; dragOffsetY is that one task's
    // live, cumulative finger movement in px, applied as a visual translation. displayedTasks is
    // the on-screen order - normally just [visibleTasks], but during a drag it's the optimistic,
    // already-swapped order so cards visibly shift before the reorder is persisted.
    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var displayedTasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }

    // Applies a raw Y delta (px) to whichever task is currently being dragged - shared by both
    // the real pointer-drag callback below and the edge auto-scroll effect further down, so a
    // list scroll driven by auto-scroll runs through the exact same offset+swap pipeline a real
    // finger movement does, rather than a second, separate reorder path.
    val density = LocalDensity.current
    fun applyDragDelta(taskId: String, deltaY: Float) {
        dragOffsetY += deltaY
        val (reordered, correctedOffset) = dragSwapIfNeeded(
            tasks = displayedTasks,
            draggedTaskId = taskId,
            dragOffsetY = dragOffsetY,
            listState = listState
        )
        displayedTasks = reordered
        dragOffsetY = correctedOffset
    }

    // Continuous edge auto-scroll: while a drag is active and the dragged card's current
    // (post-offset) position is within AUTO_SCROLL_EDGE_ZONE_DP of the top/bottom of the list's
    // own viewport, scrolls the list toward that edge every frame - faster the closer the card
    // is to the actual edge - so the user can drag from the bottom of a long list to the top (or
    // vice versa) as one continuous gesture instead of having to drop, manually scroll, and
    // re-pick-up the task. Every scrolled pixel is immediately fed back through applyDragDelta
    // (as a compensating delta) so the dragged card's on-screen position stays pinned under the
    // user's finger while the list moves underneath it, exactly like dragSwapIfNeeded
    // already keeps it continuous across a swap. Restarts automatically for each new drag (keyed
    // on draggedTaskId) and stops the instant the drag ends, since draggedTaskId becoming null
    // cancels this effect.
    LaunchedEffect(draggedTaskId) {
        val taskId = draggedTaskId ?: return@LaunchedEffect
        val edgeZonePx = with(density) { AUTO_SCROLL_EDGE_ZONE_DP.dp.toPx() }
        val minSpeedPx = with(density) { AUTO_SCROLL_MIN_SPEED_DP_PER_SEC.dp.toPx() }
        val maxSpeedPx = with(density) { AUTO_SCROLL_MAX_SPEED_DP_PER_SEC.dp.toPx() }
        var lastFrameTimeNanos = -1L
        while (isActive) {
            val frameTimeNanos = withFrameNanos { it }
            val deltaSeconds = if (lastFrameTimeNanos < 0) {
                0f
            } else {
                (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
            }
            lastFrameTimeNanos = frameTimeNanos

            val info = listState.layoutInfo
            val draggedInfo = info.visibleItemsInfo.find { it.key == taskId } ?: continue
            val effectiveTop = draggedInfo.offset + dragOffsetY
            val effectiveBottom = effectiveTop + draggedInfo.size
            val topIntrusion = (info.viewportStartOffset + edgeZonePx) - effectiveTop
            val bottomIntrusion = effectiveBottom - (info.viewportEndOffset - edgeZonePx)

            val direction: Float
            val intrusion: Float
            when {
                topIntrusion > 0f -> {
                    direction = -1f
                    intrusion = topIntrusion
                }
                bottomIntrusion > 0f -> {
                    direction = 1f
                    intrusion = bottomIntrusion
                }
                else -> {
                    direction = 0f
                    intrusion = 0f
                }
            }

            if (direction != 0f && deltaSeconds > 0f) {
                val speedPxPerSec = minSpeedPx +
                    (maxSpeedPx - minSpeedPx) * (intrusion / edgeZonePx).coerceIn(0f, 1f)
                val consumed = listState.scrollBy(direction * speedPxPerSec * deltaSeconds)
                if (consumed != 0f) {
                    // A LazyListItemInfo.offset is viewport-relative, so it shifts by -consumed
                    // the instant the list scrolls - the item's slot moves the opposite way the
                    // content scrolled. dragOffsetY must absorb exactly that shift (+consumed) so
                    // the drawn position (offset + dragOffsetY, see the graphicsLayer translationY
                    // below) stays fixed on screen under the stationary finger while the list
                    // moves underneath it, instead of drifting by 2x consumed every frame.
                    applyDragDelta(taskId, consumed)
                }
            }
        }
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    // All/Basic/Timer are filtered VIEWS over one single shared manual order (orderInAll) -
    // filtering never re-sorts the remaining tasks, it only narrows which of them are shown, per
    // spec. Basic/Timer classification depends only on whether a timer is set - Reminder,
    // Category, Priority, Subtasks, Repeat, and Pending Task never affect it. Within that
    // filtered set, completed tasks are sunk below active ones (each group keeps its own
    // orderInAll ordering) rather than being a separate tab/scope the way Done used to be - so
    // completing/un-completing a task only ever moves it between these two groups, never touches
    // orderInAll itself, and un-completing naturally restores its prior position.
    val visibleTasks = tasks
        .filter { task ->
            when (val filter = selectedFilter) {
                TaskListFilter.ALL -> true
                TaskListFilter.BASIC -> task.timerMinutes == null
                TaskListFilter.TIMER -> task.timerMinutes != null
                is TaskListFilter.CATEGORY -> task.categoryId == filter.categoryId
            }
        }
        .partition { it.status == TaskStatus.COMPLETED }
        .let { (completed, active) -> active.sortedBy { it.orderInAll } + completed.sortedBy { it.orderInAll } }

    // Only re-sync from the real (persisted) order while nothing is actively being dragged, so a
    // fresh Flow emission mid-drag can't yank the list back to the pre-drag order under the
    // user's finger.
    LaunchedEffect(visibleTasks, draggedTaskId) {
        if (draggedTaskId == null) {
            displayedTasks = visibleTasks
        }
    }

    // Task is one of only 3 screens the wallpaper IMAGE itself is scoped to (see the Wallpaper
    // spec's "image scope" rule) - WallpaperBackdrop is a no-op when no wallpaper is selected, so
    // this Box changes nothing about this screen's existing look/behavior in that case. Scaffold's
    // own containerColor is made transparent only while a wallpaper is active, so the image
    // actually shows through instead of being covered by the screen's ordinary solid background.
    Box(modifier = Modifier.fillMaxSize()) {
    WallpaperBackdrop(wallpaper = wallpaper, darkTheme = darkTheme)
    Scaffold(
        containerColor = if (wallpaper == Wallpaper.NONE) MaterialTheme.colorScheme.background else Color.Transparent,
        bottomBar = {
            OneTaskBottomNav(
                activeTab = BottomNavTab.TASKS,
                onJournalClick = onNavigateToJournal,
                onTasksClick = {},
                onTimerClick = onOpenTimerPlaceholder,
                backgroundColor = OneTaskWallpapers.definitionFor(wallpaper)?.let {
                    if (darkTheme) it.dark.bottomNavigation else it.light.bottomNavigation
                } ?: MaterialTheme.colorScheme.surface,
                elevated = wallpaper != Wallpaper.NONE
            )
        },
        floatingActionButton = {
            val fabDensity = LocalDensity.current
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    fabHeight = with(fabDensity) { coordinates.size.height.toDp() }
                }
            ) {
                OneTaskAddButton(
                    onClick = onAddTaskClick,
                    contentDescription = stringResource(id = R.string.add_task),
                    wallpaper = wallpaper
                )
            }
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
                TaskScreenHeader(
                    profilePhotoPath = profilePhotoPath,
                    selectedDate = selectedDate,
                    onAvatarClick = onProfileAvatarClick,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onCalendarClick = { showDatePicker = true },
                    wallpaper = wallpaper
                )

                TaskFilterStrip(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    onCategoryFilterClick = { showCategoryFilterSelector = true },
                    modifier = Modifier.padding(top = 16.dp)
                )

                if (visibleTasks.isEmpty()) {
                    HomeEmptyState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 16.dp),
                        // contentPadding (not a Modifier.padding, which would shrink the
                        // scrollable viewport itself) lets the last card scroll fully clear of
                        // the FAB while leaving the drag-and-drop auto-scroll effect's own
                        // viewport-edge math (see AUTO_SCROLL_EDGE_ZONE_DP above) untouched -
                        // that effect reads listState.layoutInfo's viewport bounds, which
                        // contentPadding doesn't change.
                        contentPadding = PaddingValues(
                            bottom = fabHeight + FAB_SCAFFOLD_END_MARGIN_DP.dp + FAB_SAFETY_GAP_DP.dp
                        ),
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
                                onDrag = { deltaY -> applyDragDelta(task.id, deltaY) },
                                onDragEnd = {
                                    val finalOrder = displayedTasks
                                    draggedTaskId = null
                                    dragOffsetY = 0f
                                    // All/Basic/Timer share one manual order (see visibleTasks
                                    // above), so a reorder from any of them always persists to
                                    // the same ALL scope - unchanged drag-and-drop mechanics,
                                    // just always targeting the one scope that now exists.
                                    viewModel.reorderTasks(TaskOrderScope.ALL, finalOrder)
                                },
                                listState = listState,
                                modifier = if (isDragged) Modifier else Modifier.animateItem(),
                                wallpaper = wallpaper,
                                customCategories = customCategories
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showCategoryFilterSelector) {
        CategorySelectorDialog(
            currentCategoryId = (selectedFilter as? TaskListFilter.CATEGORY)?.categoryId,
            customCategories = customCategories,
            onDismiss = { showCategoryFilterSelector = false },
            onConfirm = { chosen ->
                selectedFilter = TaskListFilter.CATEGORY(chosen)
                showCategoryFilterSelector = false
            },
            onAddCategoryClick = {
                showCategoryFilterSelector = false
                onAddCategoryClick()
            }
        )
    }

    if (showDatePicker) {
        val datesWithTasks by viewModel.datesWithTasksInCalendarMonth.collectAsState()
        OneTaskCalendarDialog(
            selectedDate = selectedDate,
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false },
            markedDates = datesWithTasks,
            onVisibleMonthChanged = viewModel::setCalendarVisibleMonth,
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
 * The Tasks screen's compact header: profile avatar, previous-day, the selected date, next-day,
 * and the calendar entry point, all in one row - replacing the old two-row header (branding
 * title/tagline centered above a separate date-navigation row) per the UI revamp. Every callback
 * here is the exact same one the old two-row layout already called; only the arrangement changed
 * - date-navigation and calendar/profile access all still work exactly as before.
 */
@Composable
private fun TaskScreenHeader(
    profilePhotoPath: String?,
    selectedDate: LocalDate,
    onAvatarClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val profileDescription = stringResource(id = R.string.nav_profile)
    val calendarDescription = stringResource(id = R.string.calendar)
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
        modifier = modifier
            .fillMaxWidth()
            // Only while a wallpaper is active: gives this row's avatar/date/nav icons the same
            // translucent-surface-plus-border treatment as Cards elsewhere on this screen, since
            // (unlike a Card) this header previously rendered directly over the wallpaper image
            // with nothing behind it - reusing the existing Verdant border/surface tokens, not a
            // new color. Non-wallpaper themes are unaffected.
            .then(
                if (wallpaper != Wallpaper.NONE) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAvatarClick,
            modifier = Modifier.semantics { contentDescription = profileDescription }
        ) {
            ProfileAvatar(photoPath = profilePhotoPath, size = 32.dp)
        }

        IconButton(onClick = onPreviousDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.previous_day),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = relativeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onNextDay) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.next_day),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier.semantics { contentDescription = calendarDescription }
        ) {
            OneTaskCalendarIcon(tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * The compact filter strip under date navigation: All/Basic/Timer/Category. All/Basic/Timer
 * drive [TaskListFilter] directly (see its own doc comment for the exact All/Basic/Timer
 * classification rule, unchanged by Category). Tapping Category never toggles a filter by
 * itself - it always calls [onCategoryFilterClick] to open the Category selector (see
 * HomeScreen's own [CategorySelectorDialog] usage), which is what actually applies a
 * [TaskListFilter.CATEGORY] value via [onFilterSelected]. The pill shows selected whenever
 * [selectedFilter] is already CATEGORY, regardless of which category it's narrowed to.
 */
@Composable
private fun TaskFilterStrip(
    selectedFilter: TaskListFilter,
    onFilterSelected: (TaskListFilter) -> Unit,
    onCategoryFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        TaskListFilter.ALL to stringResource(id = R.string.tab_all),
        TaskListFilter.BASIC to stringResource(id = R.string.task_filter_basic),
        TaskListFilter.TIMER to stringResource(id = R.string.task_filter_timer)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            FilterPill(
                text = label,
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
        FilterPill(
            text = stringResource(id = R.string.task_filter_category),
            selected = selectedFilter is TaskListFilter.CATEGORY,
            onClick = onCategoryFilterClick
        )
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/**
 * Centered, calm empty state - the existing Tasks-tab glyph ([OneTaskTasksIcon], reused rather
 * than a new illustration asset) on a soft tonal backdrop, plus the same title/subtitle text this
 * screen already used. Shown identically whether the whole day has no tasks or the current
 * All/Basic/Timer filter simply has no matches within an otherwise non-empty day - no task data
 * is ever fabricated to avoid this state.
 */
@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                OneTaskTasksIcon(active = false, size = 48.dp)
            }
            Text(
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = stringResource(id = R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
    listState: LazyListState,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE,
    customCategories: List<CategoryEntity> = emptyList()
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
        listState = listState,
        modifier = modifier,
        wallpaper = wallpaper,
        customCategories = customCategories
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
    listState: LazyListState,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE,
    customCategories: List<CategoryEntity> = emptyList()
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
                modifier = Modifier.padding(bottom = 8.dp),
                wallpaper = wallpaper
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
            onDragEnd = onDragEnd,
            listState = listState,
            wallpaper = wallpaper,
            customCategories = customCategories
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
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE
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
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            // Same translucent Card surface Timer's own pills/segmented control already pair
            // with a MaterialTheme.colorScheme.outline border for definition against the
            // wallpaper backdrop - only while a wallpaper is actually active, so non-wallpaper
            // themes (whose Card-like surfaces are already opaque) are unaffected.
            border = if (wallpaper != Wallpaper.NONE) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            } else {
                null
            }
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
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
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
        color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
    onDragEnd: () -> Unit,
    listState: LazyListState,
    wallpaper: Wallpaper = Wallpaper.NONE,
    customCategories: List<CategoryEntity> = emptyList()
) {
    var subtasksExpanded by remember(task.id) { mutableStateOf(false) }
    val isCompleted = task.status == TaskStatus.COMPLETED
    val subtasksInteractive = task.timerMinutes == null

    // When expanding reveals subtasks that would otherwise run off the bottom of the visible
    // list area (e.g. under the bottom navigation, which the Scaffold already keeps the list's
    // own viewport clear of), scroll up just enough to bring them into view. Waits a couple of
    // frames first so this card's newly-expanded height has actually been measured and laid out
    // before reading listState.layoutInfo - reading it too early would still see the pre-expansion
    // size. Never fires on collapse, and never scrolls the card's own header off the top: for a
    // subtask list taller than the whole viewport, this settles with the header pinned at the top
    // and the rest reachable by the user's own scroll, exactly as before.
    LaunchedEffect(subtasksExpanded) {
        if (!subtasksExpanded) return@LaunchedEffect
        repeat(2) { withFrameNanos {} }
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.find { it.key == task.id } ?: return@LaunchedEffect
        val overflow = (item.offset + item.size) - info.viewportEndOffset
        if (overflow > 0) {
            val maxScroll = (item.offset - info.viewportStartOffset).coerceAtLeast(0)
            val scrollAmount = overflow.coerceAtMost(maxScroll).toFloat()
            if (scrollAmount > 0f) {
                listState.animateScrollBy(scrollAmount)
            }
        }
    }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Only a very slight elevation bump communicates "this card is now draggable" - no
        // scale, rotation, or size change.
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 4.dp else 1.dp),
        // Same translucent Card surface Timer's own pills/segmented control already pair with a
        // MaterialTheme.colorScheme.outline border for definition against the wallpaper backdrop
        // - only while a wallpaper is actually active, so non-wallpaper themes (whose Card
        // containerColor is already opaque) are unaffected.
        border = if (wallpaper != Wallpaper.NONE) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            null
        }
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
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                // Only shown when the task actually has a Category assigned - categoryDisplayName
                // itself would fall back to a "No Category" string for a null id, which is exactly
                // the case this card must show nothing for instead.
                task.categoryId?.let { categoryId ->
                    TagPill(
                        text = categoryDisplayName(categoryId, customCategories),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                priorityLabelRes(task.priority)?.let { labelRes ->
                    Spacer(modifier = Modifier.width(12.dp))
                    TagPill(text = stringResource(id = labelRes))
                }

                task.timerMinutes?.let { minutes ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.timer_minutes_format, minutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (task.subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.subtasks_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
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

/** null for [TaskPriority.NONE] (no priority selected, nothing shown on the card) - Small/
 * Medium/High otherwise. Priority is purely informational: it never affects task ordering,
 * filtering, or which tab a task appears in. */
private fun priorityLabelRes(priority: TaskPriority): Int? = when (priority) {
    TaskPriority.NONE -> null
    TaskPriority.SMALL -> R.string.priority_small
    TaskPriority.MEDIUM -> R.string.priority_medium
    TaskPriority.HIGH -> R.string.priority_high
}

@Composable
private fun TagPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
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
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .then(if (interactive) Modifier.clickable(onClick = onToggle) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (subtask.completed) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = subtask.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
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
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
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
                color = MaterialTheme.colorScheme.primary
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
        containerColor = MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.delete_task_confirm_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
