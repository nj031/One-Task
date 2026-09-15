package com.nj031.onetask.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.focus.FocusSessionState
import com.nj031.onetask.data.task.Subtask
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.viewmodel.HomeViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MILLIS_PER_MINUTE = 60_000L
private const val TICK_INTERVAL_MILLIS = 250L

@Composable
fun FocusTimerScreen(
    viewModel: HomeViewModel = viewModel(),
    taskId: String,
    onBackToHome: () -> Unit = {}
) {
    val task by remember(taskId) { viewModel.observeTask(taskId) }
        .collectAsState(initial = null)
    var showBreakConfirm by remember { mutableStateOf(false) }
    var showLeaveFocusConfirm by remember { mutableStateOf(false) }

    // Whether the "Focus session complete" prompt (for a finished timer with subtasks still
    // left) has been dismissed via Continue Task. Reset below whenever a fresh countdown
    // actually starts, so a genuinely new completion later correctly re-prompts.
    var completionAcknowledged by remember(taskId) { mutableStateOf(false) }

    // Android/system Back must never jump straight out of Focus Mode: an already-open
    // confirmation panel is closed first, and only a bare Back (no panel open) opens the
    // leave-focus confirmation. Only one of these is ever enabled at a time, so registration
    // order doesn't matter.
    BackHandler(enabled = showBreakConfirm) { showBreakConfirm = false }
    BackHandler(enabled = showLeaveFocusConfirm) { showLeaveFocusConfirm = false }
    BackHandler(enabled = !showBreakConfirm && !showLeaveFocusConfirm) { showLeaveFocusConfirm = true }

    val context = LocalContext.current

    // Marks this task as the open, not-yet-exited Focus Mode session so the app can relaunch
    // straight back into it after the process is killed in the background (see
    // FocusSessionState). Only Break, Leave Focus and reaching Done - via exitFocusMode below -
    // clear it; simply backgrounding the app, locking the screen or losing the process must not.
    LaunchedEffect(taskId) { FocusSessionState.setActive(context, taskId) }
    val exitFocusMode: () -> Unit = {
        FocusSessionState.clearActive(context)
        onBackToHome()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: the timer itself is driven by a stored end-timestamp, not by the notification. */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A fresh task (never opened before) enters In Progress and starts counting
    // down the moment this screen is shown.
    LaunchedEffect(task?.id, task?.status) {
        val current = task
        if (current != null && current.status == TaskStatus.NOT_STARTED) {
            viewModel.startTimer(current)
        }
    }

    // Purely local, UI-only clock used to recompute remaining time every tick.
    // The source of truth is always (timerEndAtMillis - actual current time),
    // never an assumed one-second-per-tick decrement.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(task?.timerEndAtMillis) {
        completionAcknowledged = false
        val endAt = task?.timerEndAtMillis ?: return@LaunchedEffect
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            if (current >= endAt) {
                task?.let { viewModel.finishTimer(it) }
                break
            }
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    // The active timer's own session is over the moment the task is marked Done - whether
    // that happened automatically (no subtasks left) or via the completion prompt's "Mark Task
    // Done" button - so leaving Focus Mode is handled in exactly one place for both cases.
    LaunchedEffect(task?.status) {
        if (task?.status == TaskStatus.COMPLETED) exitFocusMode()
    }

    val hasIncompleteSubtasks = task?.subtasks?.any { !it.completed } ?: false
    val isSessionComplete = task?.let {
        it.timerMinutes != null &&
            it.timerEndAtMillis == null &&
            it.timerRemainingMillis == 0L &&
            it.status == TaskStatus.IN_PROGRESS
    } ?: false

    // Auto-completion: nothing left to finish, so there's nothing for the user to decide.
    // Self-limiting - markTaskDone flips status away from IN_PROGRESS, which makes
    // isSessionComplete false on the next recomposition.
    LaunchedEffect(isSessionComplete, hasIncompleteSubtasks) {
        val current = task
        if (current != null && isSessionComplete && !hasIncompleteSubtasks) {
            viewModel.markTaskDone(current)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
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
                FocusTimerTopBar()

                task?.let { currentTask ->
                    // Keyed on the task's id AND its currently configured timer duration: if the
                    // user edits the duration (e.g. while paused on a break), this forces the ring
                    // to be fully torn down and redrawn from the new duration, rather than risking
                    // any stale draw state carried over from the previous duration.
                    key(currentTask.id, currentTask.timerMinutes) {
                        FocusTimerCard(
                            task = currentTask,
                            nowMillis = nowMillis,
                            showCompletionPrompt = isSessionComplete && hasIncompleteSubtasks && !completionAcknowledged,
                            onPauseOrResume = {
                                if (currentTask.timerEndAtMillis != null) {
                                    viewModel.pauseTimer(currentTask)
                                } else {
                                    viewModel.startTimer(currentTask)
                                }
                            },
                            onReset = { viewModel.resetTimer(currentTask) },
                            onBreak = { showBreakConfirm = true },
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(currentTask, subtaskId) },
                            onMarkTaskDone = { viewModel.markTaskDone(currentTask) },
                            onContinueTask = { completionAcknowledged = true },
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
            }
        }

        val currentTask = task
        if (showBreakConfirm && currentTask != null) {
            BreakConfirmationSheet(
                onTakeBreak = {
                    viewModel.pauseTimer(currentTask)
                    showBreakConfirm = false
                    exitFocusMode()
                },
                onCancel = { showBreakConfirm = false }
            )
        }
        if (showLeaveFocusConfirm && currentTask != null) {
            LeaveFocusConfirmationSheet(
                onLeaveFocus = {
                    viewModel.pauseTimer(currentTask)
                    showLeaveFocusConfirm = false
                    exitFocusMode()
                },
                onCancel = { showLeaveFocusConfirm = false }
            )
        }
    }
}

@Composable
private fun FocusTimerTopBar() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.focus_timer_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun FocusTimerCard(
    task: TaskEntity,
    nowMillis: Long,
    showCompletionPrompt: Boolean,
    onPauseOrResume: () -> Unit,
    onReset: () -> Unit,
    onBreak: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onMarkTaskDone: () -> Unit,
    onContinueTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    // totalMillis always reflects the task's CURRENTLY configured duration (task.timerMinutes),
    // never a duration captured at some earlier point. remainingMillis is always an absolute
    // duration (preserved verbatim across pause/resume/edit).
    val totalMillis = (task.timerMinutes ?: 0) * MILLIS_PER_MINUTE
    val remainingMillis = when {
        task.timerEndAtMillis != null -> (task.timerEndAtMillis - nowMillis).coerceAtLeast(0)
        task.timerRemainingMillis != null -> task.timerRemainingMillis
        else -> totalMillis
    }
    val isRunning = task.timerEndAtMillis != null
    val isCompleted = task.status == TaskStatus.COMPLETED
    // "Resume" only makes sense once there's actually a paused, partway-through session to
    // resume (i.e. the task is still In Progress). Right after Reset the task is back to Not
    // Started at the full duration, so the same button must read "Start" instead - otherwise it
    // wrongly implies a stopped-but-in-progress timer that Reset just cleared.
    val primaryButtonLabelRes = when {
        isRunning -> R.string.pause
        task.status == TaskStatus.NOT_STARTED -> R.string.task_action_start
        else -> R.string.resume
    }
    // Elapsed fraction of the configured duration: 0% the moment a timer is fresh/reset, 100%
    // once it has fully run out. The ring, its progress arc and the moving dot all derive from
    // this single value, so they can never disagree with one another.
    val progress = if (totalMillis > 0) {
        ((totalMillis - remainingMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remainingSubtasksCount = task.subtasks.count { !it.completed }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FocusTimerRing(
                progress = progress,
                remainingLabel = formatHms(remainingMillis),
                modifier = Modifier.size(220.dp)
            )

            Text(
                text = stringResource(id = R.string.focus_timer_total_format, formatHms(totalMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (isCompleted) {
                Text(
                    text = stringResource(id = R.string.times_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (showCompletionPrompt) {
                FocusSessionCompletePanel(
                    remainingSubtasksCount = remainingSubtasksCount,
                    onMarkTaskDone = onMarkTaskDone,
                    onContinueTask = onContinueTask,
                    modifier = Modifier.padding(top = 28.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPauseOrResume,
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = primaryButtonLabelRes))
                    }
                    FilledTonalButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.reset))
                    }
                    FilledTonalButton(
                        onClick = onBreak,
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.break_label))
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.task_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (task.subtasks.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.subtasks_section_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                )
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    task.subtasks.forEach { subtask ->
                        FocusSubtaskRow(
                            subtask = subtask,
                            onToggle = { onToggleSubtask(subtask.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusSessionCompletePanel(
    remainingSubtasksCount: Int,
    onMarkTaskDone: () -> Unit,
    onContinueTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.focus_session_complete_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.focus_session_remaining_subtasks, remainingSubtasksCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onContinueTask,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.continue_task))
            }
            FilledTonalButton(
                onClick = onMarkTaskDone,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.mark_task_done))
            }
        }
    }
}

@Composable
private fun FocusSubtaskRow(subtask: Subtask, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
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
            color = if (subtask.completed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/**
 * A circular progress ring: a faint full track, a blue progress arc, and a moving dot at the
 * arc's leading edge, all driven by the SAME [progress] value (0f = just started/reset, 1f =
 * finished) so they can never disagree with one another or show a stale ratio.
 */
@Composable
private fun FocusTimerRing(
    progress: Float,
    remainingLabel: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            val sweepAngle = 360f * clampedProgress
            if (sweepAngle > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                val endAngleRadians = Math.toRadians((-90f + sweepAngle).toDouble())
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val dotCenter = Offset(
                    x = center.x + radius * cos(endAngleRadians).toFloat(),
                    y = center.y + radius * sin(endAngleRadians).toFloat()
                )
                drawCircle(color = progressColor, radius = strokeWidthPx * 0.85f, center = dotCenter)
            }
        }
        Text(
            text = remainingLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakConfirmationSheet(
    onTakeBreak: () -> Unit,
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
                text = stringResource(id = R.string.break_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.break_confirm_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            FilledTonalButton(
                onClick = { dismissThen(onTakeBreak) },
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(id = R.string.take_a_break), style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveFocusConfirmationSheet(
    onLeaveFocus: () -> Unit,
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
                text = stringResource(id = R.string.leave_focus_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.leave_focus_confirm_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            FilledTonalButton(
                onClick = { dismissThen(onLeaveFocus) },
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(id = R.string.leave_focus), style = MaterialTheme.typography.bodyMedium)
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

private fun formatHms(totalMillis: Long): String {
    val totalSeconds = (totalMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
