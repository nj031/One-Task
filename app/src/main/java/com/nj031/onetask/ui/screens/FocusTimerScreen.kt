package com.nj031.onetask.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MILLIS_PER_MINUTE = 60_000L
private const val TICK_INTERVAL_MILLIS = 250L

@Composable
fun FocusTimerScreen(
    viewModel: HomeViewModel = viewModel(),
    taskId: String,
    onNavigateToJournal: () -> Unit = {}
) {
    val task by remember(taskId) { viewModel.observeTask(taskId) }
        .collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
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
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = formatHms(remainingMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

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
                    onClick = { /* no-op: no Break system exists elsewhere in the app yet */ },
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

private fun formatHms(totalMillis: Long): String {
    val totalSeconds = (totalMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
