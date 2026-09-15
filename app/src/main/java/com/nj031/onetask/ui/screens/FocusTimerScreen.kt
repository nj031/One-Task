package com.nj031.onetask.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskStatus
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
    onNavigateToJournal: () -> Unit = {},
    onBackToHome: () -> Unit = {}
) {
    val task by remember(taskId) { viewModel.observeTask(taskId) }
        .collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showBreakConfirm by remember { mutableStateOf(false) }

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
        val endAt = task?.timerEndAtMillis ?: return@LaunchedEffect
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            if (current >= endAt) {
                task?.let { viewModel.completeTimer(it) }
                break
            }
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.6f)) {
                AppDrawerContent(
                    userEmail = stringResource(id = R.string.sample_user_email),
                    onJournalingClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToJournal()
                    },
                    onSettingsClick = { /* no-op: settings not implemented yet */ },
                    onLogoutClick = { /* no-op: logout not implemented yet */ }
                )
            }
        }
    ) {
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
                    FocusTimerTopBar(onMenuClick = { scope.launch { drawerState.open() } })

                    task?.let { currentTask ->
                        FocusTimerCard(
                            task = currentTask,
                            nowMillis = nowMillis,
                            onPauseOrResume = {
                                if (currentTask.timerEndAtMillis != null) {
                                    viewModel.pauseTimer(currentTask)
                                } else {
                                    viewModel.startTimer(currentTask)
                                }
                            },
                            onReset = { viewModel.resetTimer(currentTask) },
                            onBreak = { showBreakConfirm = true },
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
            }

            val currentTask = task
            if (showBreakConfirm && currentTask != null) {
                BreakConfirmationSheet(
                    onTakeBreak = {
                        viewModel.pauseTimer(currentTask)
                        showBreakConfirm = false
                        onBackToHome()
                    },
                    onCancel = { showBreakConfirm = false }
                )
            }
        }
    }
}

@Composable
private fun FocusTimerTopBar(onMenuClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(id = R.string.menu),
                tint = MaterialTheme.colorScheme.primary
            )
        }

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
    onPauseOrResume: () -> Unit,
    onReset: () -> Unit,
    onBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMillis = (task.timerMinutes ?: 0) * MILLIS_PER_MINUTE
    val remainingMillis = when {
        task.timerEndAtMillis != null -> (task.timerEndAtMillis - nowMillis).coerceAtLeast(0)
        task.timerRemainingMillis != null -> task.timerRemainingMillis
        else -> totalMillis
    }
    val isRunning = task.timerEndAtMillis != null
    val isCompleted = task.status == TaskStatus.COMPLETED
    val progress = if (totalMillis > 0) {
        (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
                    Text(
                        text = stringResource(
                            id = if (isRunning) R.string.pause else R.string.resume
                        )
                    )
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
        }
    }
}

/**
 * A circular timer ring whose blue progress arc sweeps anti-clockwise from the top as time
 * elapses, with a dot drawn exactly at the arc's endpoint so the two never fall out of sync.
 */
@Composable
private fun FocusTimerRing(
    progress: Float,
    remainingLabel: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

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

            val clampedProgress = progress.coerceIn(0f, 1f)
            val sweepAngle = -360f * clampedProgress
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            if (clampedProgress > 0.001f) {
                val endAngleRadians = Math.toRadians((-90f + sweepAngle).toDouble())
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val dotCenter = Offset(
                    x = center.x + radius * cos(endAngleRadians).toFloat(),
                    y = center.y + radius * sin(endAngleRadians).toFloat()
                )
                drawCircle(
                    color = progressColor,
                    radius = strokeWidthPx * 0.85f,
                    center = dotCenter
                )
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
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.break_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.break_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            FilledTonalButton(
                onClick = onTakeBreak,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(stringResource(id = R.string.take_a_break))
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
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
