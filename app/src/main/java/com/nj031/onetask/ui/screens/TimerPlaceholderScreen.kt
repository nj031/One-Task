package com.nj031.onetask.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.data.timer.TimerMode
import com.nj031.onetask.data.timer.TimerSessionSnapshot
import com.nj031.onetask.data.timer.formatTimerDuration
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.components.WallpaperBackdrop
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskClockIcon
import com.nj031.onetask.ui.theme.OneTaskLapFlagIcon
import com.nj031.onetask.ui.theme.OneTaskPauseIcon
import com.nj031.onetask.ui.theme.OneTaskPlayIcon
import com.nj031.onetask.ui.theme.OneTaskStopIcon
import com.nj031.onetask.ui.theme.OneTaskStopwatchIcon
import com.nj031.onetask.ui.theme.OneTaskWallpapers
import com.nj031.onetask.viewmodel.TimerViewModel
import kotlin.math.cos
import kotlin.math.sin

private enum class TimerTab { TIMER, STOPWATCH }

private val TIMER_PRESET_MINUTES = listOf(5, 25, 45, 60)

/**
 * The Timer tab: a Timer/Stopwatch screen reached from the bottom nav's middle tab. Both modes
 * share this one screen (a segmented control switches between them); the actual countdown/
 * elapsed-time session survives backgrounding or the app being killed outright, since the real
 * state lives in TimerSessionRepository (recovered by TimerViewModel) and StandaloneTimer-
 * ForegroundService keeps it moving - and its notification updating - independently of whether
 * this screen is on screen at all. Focus Mode's own separate timer/notification implementation
 * (FocusTimerScreen/TimerForegroundService) is untouched by any of this.
 */
@Composable
fun TimerPlaceholderScreen(
    viewModel: TimerViewModel = viewModel(),
    onNavigateToJournal: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onTimerSettingsClick: () -> Unit = {},
    onCustomDurationSettingsClick: () -> Unit = {},
    onTimerHistoryClick: () -> Unit = {},
    onFocusModeClick: () -> Unit = {},
    wallpaper: Wallpaper = Wallpaper.NONE,
    darkTheme: Boolean = false
) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsState()
    val selectedIdleDurationMillis by viewModel.selectedIdleDurationMillis.collectAsState()

    // Same runtime-permission request FocusTimerScreen already performs: a one-time ask, silently
    // no-op if already granted/denied - the session itself is unaffected either way, since the
    // service's postNotification already tolerates a missing permission.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The segmented control always reflects whichever mode currently has an active (running or
    // paused) session - defaulting to Timer, per spec, whenever neither does. This is also what
    // makes the mutual-exclusion lock coherent: the tab you can freely leave is always the idle
    // one, and the tab you land back on (including after a cold start recovering a still-running
    // background Stopwatch) is always the active one.
    var selectedTab by remember {
        mutableStateOf(if (snapshot.activeMode == TimerMode.STOPWATCH) TimerTab.STOPWATCH else TimerTab.TIMER)
    }
    LaunchedEffect(snapshot.activeMode) {
        when (snapshot.activeMode) {
            TimerMode.TIMER -> selectedTab = TimerTab.TIMER
            TimerMode.STOPWATCH -> selectedTab = TimerTab.STOPWATCH
            null -> {}
        }
    }

    var showCustomDurationPicker by remember { mutableStateOf(false) }

    // Timer is one of only 3 screens the wallpaper IMAGE itself is scoped to (see the Wallpaper
    // spec's "image scope" rule) - WallpaperBackdrop is a no-op when no wallpaper is selected, so
    // this Box changes nothing about this screen's existing look/behavior in that case.
    Box(modifier = Modifier.fillMaxSize()) {
    WallpaperBackdrop(wallpaper = wallpaper, darkTheme = darkTheme)
    Scaffold(
        containerColor = if (wallpaper == Wallpaper.NONE) MaterialTheme.colorScheme.background else Color.Transparent,
        bottomBar = {
            OneTaskBottomNav(
                activeTab = BottomNavTab.TIMER,
                onJournalClick = onNavigateToJournal,
                onTasksClick = onNavigateToTasks,
                onTimerClick = {},
                backgroundColor = OneTaskWallpapers.definitionFor(wallpaper)?.let {
                    if (darkTheme) it.dark.bottomNavigation else it.light.bottomNavigation
                } ?: MaterialTheme.colorScheme.surface,
                elevated = wallpaper != Wallpaper.NONE
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                TimerTopBar(
                    onNotificationSettingsClick = onNotificationSettingsClick,
                    onTimerSettingsClick = onTimerSettingsClick,
                    onCustomDurationSettingsClick = onCustomDurationSettingsClick,
                    onTimerHistoryClick = onTimerHistoryClick,
                    wallpaper = wallpaper
                )

                TimerStopwatchSegmentedControl(
                    selectedTab = selectedTab,
                    timerLocked = snapshot.activeMode == TimerMode.STOPWATCH,
                    stopwatchLocked = snapshot.activeMode == TimerMode.TIMER,
                    onSelectTimer = { selectedTab = TimerTab.TIMER },
                    onSelectStopwatch = { selectedTab = TimerTab.STOPWATCH },
                    modifier = Modifier.padding(top = 20.dp)
                )

                when (selectedTab) {
                    TimerTab.TIMER -> TimerModeContent(
                        snapshot = snapshot,
                        selectedIdleDurationMillis = selectedIdleDurationMillis,
                        onSelectPreset = viewModel::selectIdleDuration,
                        onOpenCustomPicker = { showCustomDurationPicker = true },
                        onStart = viewModel::startTimer,
                        onPause = viewModel::pauseTimer,
                        onResume = viewModel::resumeTimer,
                        onStop = viewModel::stopTimer,
                        onFocusModeClick = onFocusModeClick,
                        wallpaper = wallpaper
                    )
                    TimerTab.STOPWATCH -> StopwatchModeContent(
                        snapshot = snapshot,
                        onStart = viewModel::startStopwatch,
                        onPause = viewModel::pauseStopwatch,
                        onResume = viewModel::resumeStopwatch,
                        onStop = viewModel::stopStopwatch,
                        wallpaper = wallpaper
                    )
                }
            }
        }
    }
    }

    if (showCustomDurationPicker) {
        OneTaskDurationPickerDialog(
            initialMillis = selectedIdleDurationMillis,
            onDismiss = { showCustomDurationPicker = false },
            onConfirm = { millis ->
                viewModel.selectIdleDuration(millis)
                showCustomDurationPicker = false
            }
        )
    }
}

@Composable
private fun TimerTopBar(
    onNotificationSettingsClick: () -> Unit,
    onTimerSettingsClick: () -> Unit,
    onCustomDurationSettingsClick: () -> Unit,
    onTimerHistoryClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Only while a wallpaper is active: gives this title/menu row the same translucent-
            // surface-plus-border treatment as this screen's own pills/segmented control, since
            // (unlike those) this header previously rendered directly over the wallpaper image
            // with nothing behind it - reusing the existing Verdant border/surface tokens, not a
            // new color. Non-wallpaper themes are unaffected.
            .then(
                if (wallpaper != Wallpaper.NONE) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = stringResource(id = R.string.timer_focus_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.timer_more_options),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                // Same translucent-surface-plus-border treatment as this screen's own pills/
                // segmented control (see TimerStopwatchSegmentedControl/TimerPresetPill) - only
                // while a wallpaper is actually active, so non-wallpaper themes are unaffected.
                modifier = if (wallpaper != Wallpaper.NONE) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.timer_menu_notification_settings)) },
                    onClick = { showMenu = false; onNotificationSettingsClick() }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.timer_menu_settings)) },
                    onClick = { showMenu = false; onTimerSettingsClick() }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.timer_menu_custom_duration)) },
                    onClick = { showMenu = false; onCustomDurationSettingsClick() }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.timer_menu_history)) },
                    onClick = { showMenu = false; onTimerHistoryClick() }
                )
            }
        }
    }
}

@Composable
private fun TimerStopwatchSegmentedControl(
    selectedTab: TimerTab,
    timerLocked: Boolean,
    stopwatchLocked: Boolean,
    onSelectTimer: () -> Unit,
    onSelectStopwatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        SegmentedTab(
            icon = { tint -> OneTaskClockIcon(tint = tint, size = 20.dp) },
            label = stringResource(id = R.string.timer_tab_timer),
            selected = selectedTab == TimerTab.TIMER,
            enabled = !timerLocked,
            onClick = onSelectTimer,
            modifier = Modifier.weight(1f)
        )
        SegmentedTab(
            icon = { tint -> OneTaskStopwatchIcon(tint = tint, size = 20.dp) },
            label = stringResource(id = R.string.timer_tab_stopwatch),
            selected = selectedTab == TimerTab.STOPWATCH,
            enabled = !stopwatchLocked,
            onClick = onSelectStopwatch,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedTab(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = when {
        selected -> MaterialTheme.colorScheme.primary
        !enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TimerModeContent(
    snapshot: TimerSessionSnapshot,
    selectedIdleDurationMillis: Long,
    onSelectPreset: (Long) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onFocusModeClick: () -> Unit = {},
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val hapticTick = rememberHapticTick()
    val isRunning = snapshot.isTimerRunning
    val isPaused = snapshot.isTimerPaused
    val isActive = isRunning || isPaused

    val totalMillis = if (isActive) snapshot.timerTotalDurationMillis else selectedIdleDurationMillis
    val displayMillis = if (isActive) snapshot.timerRemainingNowMillis() else selectedIdleDurationMillis
    val remainingFraction = if (totalMillis > 0) displayMillis.toFloat() / totalMillis.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = timerRingBackingModifier(wallpaper), contentAlignment = Alignment.Center) {
            TimerRing(remainingFraction = remainingFraction, showProgress = isActive)
            TimerCenterLabel(millis = displayMillis)
        }
    }

    if (!isActive) {
        TimerPresetRow(
            selectedMillis = selectedIdleDurationMillis,
            onSelectPreset = onSelectPreset,
            onCustomClick = onOpenCustomPicker,
            modifier = Modifier.padding(top = 28.dp)
        )
    }

    when {
        !isActive -> TimerPrimaryButton(
            icon = { OneTaskPlayIcon(tint = Color.White, size = 20.dp) },
            label = stringResource(id = R.string.timer_start_button),
            onClick = { hapticTick(); onStart() },
            wallpaper = wallpaper
        )
        isRunning -> TimerPrimaryButton(
            icon = { OneTaskPauseIcon(tint = Color.White, size = 20.dp) },
            label = stringResource(id = R.string.pause),
            onClick = { hapticTick(); onPause() },
            wallpaper = wallpaper
        )
        isPaused -> {
            TimerPrimaryButton(
                icon = { OneTaskPlayIcon(tint = Color.White, size = 20.dp) },
                label = stringResource(id = R.string.resume),
                onClick = { hapticTick(); onResume() },
                wallpaper = wallpaper
            )
            TimerSecondaryStopButton(onClick = { hapticTick(); onStop() })
        }
    }

    FocusModeButton(onClick = onFocusModeClick)
}

@Composable
private fun StopwatchModeContent(
    snapshot: TimerSessionSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val hapticTick = rememberHapticTick()
    val isRunning = snapshot.isStopwatchRunning
    val isPaused = snapshot.isStopwatchPaused
    val isActive = isRunning || isPaused
    val elapsedMillis = if (isActive) snapshot.stopwatchElapsedNowMillis() else 0L

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = timerRingBackingModifier(wallpaper), contentAlignment = Alignment.Center) {
            TimerRing(remainingFraction = 1f, showProgress = false)
            TimerCenterLabel(millis = elapsedMillis)
        }
    }

    when {
        !isActive -> TimerPrimaryButton(
            icon = { OneTaskPlayIcon(tint = Color.White, size = 20.dp) },
            label = stringResource(id = R.string.timer_start_button),
            onClick = { hapticTick(); onStart() },
            modifier = Modifier.padding(top = 28.dp),
            wallpaper = wallpaper
        )
        isRunning -> TimerPrimaryButton(
            icon = { OneTaskPauseIcon(tint = Color.White, size = 20.dp) },
            label = stringResource(id = R.string.pause),
            onClick = { hapticTick(); onPause() },
            modifier = Modifier.padding(top = 28.dp),
            wallpaper = wallpaper
        )
        isPaused -> {
            TimerPrimaryButton(
                icon = { OneTaskPlayIcon(tint = Color.White, size = 20.dp) },
                label = stringResource(id = R.string.resume),
                onClick = { hapticTick(); onResume() },
                modifier = Modifier.padding(top = 28.dp),
                wallpaper = wallpaper
            )
            TimerSecondaryStopButton(onClick = { hapticTick(); onStop() })
        }
    }

    if (isActive) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                OneTaskLapFlagIcon(tint = MaterialTheme.colorScheme.primary, size = 20.dp)
            }
        }
    }
}

/**
 * The 280dp ring+countdown-label composition's own modifier - only while a wallpaper is active,
 * adds the same translucent-surface-plus-border treatment Cards elsewhere already use, since the
 * countdown text previously rendered directly over the wallpaper image with nothing behind it.
 * Circular (matching the ring it backs) rather than the cards' rounded-rect, reusing the existing
 * Verdant border/surface tokens - not a new color, and the ring/label themselves are unchanged.
 * Non-wallpaper themes get a plain, unmodified 280dp box, exactly as before this existed.
 */
@Composable
private fun timerRingBackingModifier(wallpaper: Wallpaper): Modifier =
    if (wallpaper != Wallpaper.NONE) {
        Modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    } else {
        Modifier.size(280.dp)
    }

/**
 * The countdown/elapsed-time ring: a light full-circle track always shown, plus - only while a
 * session is actively running or paused - a blue arc representing the time still remaining
 * (Timer) which visually DISAPPEARS clockwise from a fixed 12 o'clock point as time passes, with
 * a dot marking the moving edge where it's currently "being erased". At the very start
 * (remainingFraction = 1f) the arc is the full circle; as remainingFraction shrinks toward 0 the
 * blue arc's start angle sweeps forward (clockwise) while its end always comes back around to the
 * same fixed point - the opposite of a typical "fills up" progress ring.
 */
@Composable
private fun TimerRing(remainingFraction: Float, showProgress: Boolean, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.secondaryContainer
    val progressColor = MaterialTheme.colorScheme.primary
    val clamped = remainingFraction.coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
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

        if (showProgress && clamped > 0f) {
            val elapsedFraction = 1f - clamped
            val startAngle = -90f + 360f * elapsedFraction
            val sweepAngle = 360f * clamped

            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            val dotAngleRadians = Math.toRadians(startAngle.toDouble())
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dotCenter = Offset(
                x = center.x + radius * cos(dotAngleRadians).toFloat(),
                y = center.y + radius * sin(dotAngleRadians).toFloat()
            )
            drawCircle(color = progressColor, radius = strokeWidthPx * 0.85f, center = dotCenter)
        }
    }
}

@Composable
private fun TimerCenterLabel(millis: Long) {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hasHours = totalSeconds >= 3600
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatTimerDuration(millis),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (hasHours) 28.dp else 56.dp)
        ) {
            if (hasHours) {
                Text(
                    text = stringResource(id = R.string.timer_unit_hr),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(id = R.string.timer_unit_min),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.timer_unit_sec),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerPresetRow(
    selectedMillis: Long,
    onSelectPreset: (Long) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustomSelected = TIMER_PRESET_MINUTES.none { it * 60_000L == selectedMillis }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TIMER_PRESET_MINUTES.forEach { minutes ->
            val millis = minutes * 60_000L
            TimerPresetPill(
                label = stringResource(id = R.string.timer_minutes_format, minutes),
                selected = !isCustomSelected && selectedMillis == millis,
                onClick = { onSelectPreset(millis) },
                modifier = Modifier.weight(1f)
            )
        }
        TimerPresetPill(
            label = stringResource(id = R.string.timer_preset_custom),
            selected = isCustomSelected,
            onClick = onCustomClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimerPresetPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hapticTick = rememberHapticTick()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50)
            )
            .clickable { hapticTick(); onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerPrimaryButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        // Only while a wallpaper is active: gives this CTA a defined edge against whatever
        // wallpaper pixels happen to sit behind it - the same border token/technique Cards
        // elsewhere already use - without changing containerColor's own existing opacity.
        // Non-wallpaper themes are unaffected.
        border = if (wallpaper != Wallpaper.NONE) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            null
        }
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun TimerSecondaryStopButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneTaskStopIcon(tint = MaterialTheme.colorScheme.primary, size = 16.dp)
        Text(
            text = stringResource(id = R.string.timer_stop_button),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Opens the new Focus Mode Configuration screen (UI-only Phase 1 - see FocusModeConfigScreen).
 * Does not itself start or touch anything about the actual, separate FocusTimerScreen/
 * TimerForegroundService feature. */
@Composable
private fun FocusModeButton(onClick: () -> Unit) {
    val hapticTick = rememberHapticTick()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { hapticTick(); onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.timer_focus_mode_button),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

