package com.nj031.onetask.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.data.timer.FOCUS_BREAK_DURATION_MILLIS
import com.nj031.onetask.data.timer.FocusOverlayState
import com.nj031.onetask.data.timer.TimerMode
import com.nj031.onetask.data.timer.TimerSessionSnapshot
import com.nj031.onetask.data.timer.formatTimerDuration
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.components.WallpaperBackdrop
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskClockIcon
import com.nj031.onetask.ui.theme.OneTaskCoffeeCupIcon
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
    val focusOverlay by viewModel.focusOverlay.collectAsState()
    val isFocusActive = focusOverlay != null
    // Phase 13: Strict Mode only ever gates whether the Stop action can reach
    // viewModel.stopFocusSession() early - see FocusActionMenuDialog's stopFocusingEnabled and
    // FocusRunningControls' strictModeEnabled, the two (and only) UI paths that call it.
    val strictModeEnabled = focusOverlay?.strictModeEnabled == true

    var showFocusActionMenu by remember { mutableStateOf(false) }
    var showBreakConfirm by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }
    var showTimerRunningWarning by remember { mutableStateOf(false) }

    // Running Focus screen's own Back-button behavior (per spec) - a 3-option action menu
    // (Take a break / Stop focusing / Cancel) instead of ordinary back navigation.
    BackHandler(enabled = isFocusActive) { showFocusActionMenu = true }

    val handleTakeBreakRequest: () -> Unit = {
        if ((focusOverlay?.breaksRemaining ?: 0) <= 0) {
            Toast.makeText(context, context.getString(R.string.focus_no_breaks_remaining), Toast.LENGTH_SHORT).show()
        } else {
            showBreakConfirm = true
        }
    }

    // The Focus mode button only ever renders while no session (normal or Focus) is already
    // active on the Timer tab, so the "already running" collision this guards against can only
    // ever be a normal (non-Focus) running/paused Timer.
    val handleFocusModeClick: () -> Unit = {
        if (snapshot.isTimerRunning || snapshot.isTimerPaused) {
            showTimerRunningWarning = true
        } else {
            onFocusModeClick()
        }
    }

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
                        onFocusModeClick = handleFocusModeClick,
                        focusOverlay = focusOverlay,
                        onBreakButtonClick = handleTakeBreakRequest,
                        onStopFocusButtonClick = { if (!strictModeEnabled) showStopConfirm = true },
                        onEndBreakButtonClick = viewModel::endFocusBreak,
                        wallpaper = wallpaper
                    )
                    TimerTab.STOPWATCH -> StopwatchModeContent(
                        snapshot = snapshot,
                        onStart = viewModel::startStopwatch,
                        onPause = viewModel::pauseStopwatch,
                        onResume = viewModel::resumeStopwatch,
                        onStop = viewModel::stopStopwatch,
                        focusOverlay = focusOverlay,
                        onBreakButtonClick = handleTakeBreakRequest,
                        onStopFocusButtonClick = { if (!strictModeEnabled) showStopConfirm = true },
                        onEndBreakButtonClick = viewModel::endFocusBreak,
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

    if (showFocusActionMenu) {
        FocusActionMenuDialog(
            breaksRemaining = focusOverlay?.breaksRemaining ?: 0,
            stopFocusingEnabled = !strictModeEnabled,
            onTakeBreak = { showFocusActionMenu = false; handleTakeBreakRequest() },
            onStopFocusing = {
                if (!strictModeEnabled) {
                    showFocusActionMenu = false
                    showStopConfirm = true
                }
            },
            onCancel = { showFocusActionMenu = false }
        )
    }

    if (showBreakConfirm) {
        BreakConfirmDialog(
            breaksRemaining = focusOverlay?.breaksRemaining ?: 0,
            onConfirm = { viewModel.takeFocusBreak(); showBreakConfirm = false },
            onDismiss = { showBreakConfirm = false }
        )
    }

    if (showStopConfirm) {
        StopFocusConfirmDialog(
            onConfirm = { viewModel.stopFocusSession(); showStopConfirm = false },
            onDismiss = { showStopConfirm = false }
        )
    }

    if (showTimerRunningWarning) {
        TimerRunningWarningDialog(
            onContinue = {
                showTimerRunningWarning = false
                viewModel.stopTimer()
                onFocusModeClick()
            },
            onDismiss = { showTimerRunningWarning = false }
        )
    }
}

/** The Running Focus screen's Back-button action menu (per spec): Take a break (with the dynamic
 * remaining count) / Stop focusing / Cancel - a plain rounded dialog, matching the shell every
 * other picker dialog in the app already uses (see OneTaskDurationPickerDialog/
 * CategorySelectorDialog), not a new visual pattern. */
@Composable
private fun FocusActionMenuDialog(
    breaksRemaining: Int,
    stopFocusingEnabled: Boolean,
    onTakeBreak: () -> Unit,
    onStopFocusing: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                FocusActionMenuRow(
                    title = stringResource(id = R.string.focus_action_menu_take_break),
                    subtitle = stringResource(id = R.string.focus_break_confirm_message_format, breaksRemaining),
                    onClick = onTakeBreak
                )
                // Strict Mode (Phase 13): visible but disabled - never removed from the UI, never
                // clickable, so this menu can never be used to bypass the same gate the standalone
                // Stop button enforces (see TimerModeContent/StopwatchModeContent's FocusRunningControls).
                FocusActionMenuRow(
                    title = stringResource(id = R.string.focus_action_menu_stop_focusing),
                    onClick = onStopFocusing,
                    enabled = stopFocusingEnabled
                )
                FocusActionMenuRow(
                    title = stringResource(id = R.string.cancel),
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun FocusActionMenuRow(title: String, onClick: () -> Unit, subtitle: String? = null, enabled: Boolean = true) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BreakConfirmDialog(breaksRemaining: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.focus_break_confirm_title)) },
        text = { Text(text = stringResource(id = R.string.focus_break_confirm_message_format, breaksRemaining)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.take_a_break))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun StopFocusConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.focus_stop_confirm_title)) },
        text = { Text(text = stringResource(id = R.string.focus_stop_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.timer_stop_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun TimerRunningWarningDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.focus_timer_running_warning_title)) },
        text = { Text(text = stringResource(id = R.string.focus_timer_running_warning_message)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(text = stringResource(id = R.string.continue_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
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
    focusOverlay: FocusOverlayState? = null,
    onBreakButtonClick: () -> Unit = {},
    onStopFocusButtonClick: () -> Unit = {},
    onEndBreakButtonClick: () -> Unit = {},
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val hapticTick = rememberHapticTick()
    val isRunning = snapshot.isTimerRunning
    val isPaused = snapshot.isTimerPaused
    val isActive = isRunning || isPaused
    val isOnBreak = focusOverlay?.isOnBreak == true

    val totalMillis: Long
    val displayMillis: Long
    when {
        isOnBreak -> {
            totalMillis = FOCUS_BREAK_DURATION_MILLIS
            displayMillis = focusOverlay!!.breakRemainingNowMillis()
        }
        isActive -> {
            totalMillis = snapshot.timerTotalDurationMillis
            displayMillis = snapshot.timerRemainingNowMillis()
        }
        else -> {
            totalMillis = selectedIdleDurationMillis
            displayMillis = selectedIdleDurationMillis
        }
    }
    val remainingFraction = if (totalMillis > 0) displayMillis.toFloat() / totalMillis.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = timerRingBackingModifier(wallpaper), contentAlignment = Alignment.Center) {
            TimerRing(remainingFraction = remainingFraction, showProgress = isActive || isOnBreak)
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

    if (focusOverlay != null) {
        FocusRunningControls(
            isOnBreak = isOnBreak,
            isRunning = isRunning,
            breaksRemaining = focusOverlay.breaksRemaining,
            strictModeEnabled = focusOverlay.strictModeEnabled,
            onPause = { hapticTick(); onPause() },
            onResume = { hapticTick(); onResume() },
            onBreakClick = { hapticTick(); onBreakButtonClick() },
            onStopClick = { hapticTick(); onStopFocusButtonClick() },
            onEndBreakClick = { hapticTick(); onEndBreakButtonClick() },
            wallpaper = wallpaper
        )
    } else {
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
}

/** The Running Focus screen's own bottom controls (per spec): Pause/Resume + Break while
 * running/paused (Stop only appears once paused), or a plain "on break" indicator - no buttons at
 * all, since a break always resumes automatically - while a manual break is counting down. */
@Composable
private fun FocusRunningControls(
    isOnBreak: Boolean,
    isRunning: Boolean,
    breaksRemaining: Int,
    strictModeEnabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onBreakClick: () -> Unit,
    onStopClick: () -> Unit,
    onEndBreakClick: () -> Unit,
    wallpaper: Wallpaper
) {
    if (isOnBreak) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.focus_on_break_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Same full-width secondary-action row style already used for Stop (TimerSecondaryStop-
        // Button) - the minimum addition needed to let the user resume Focus before the break's
        // own 10-minute countdown elapses on its own.
        TimerSecondaryEndBreakButton(onClick = onEndBreakClick)
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isRunning) {
            FocusHalfButton(
                icon = { OneTaskPauseIcon(tint = Color.White, size = 20.dp) },
                label = stringResource(id = R.string.pause),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                onClick = onPause,
                modifier = Modifier.weight(1f)
            )
        } else {
            FocusHalfButton(
                icon = { OneTaskPlayIcon(tint = Color.White, size = 20.dp) },
                label = stringResource(id = R.string.resume),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                onClick = onResume,
                modifier = Modifier.weight(1f)
            )
        }
        FocusHalfButton(
            icon = { OneTaskCoffeeCupIcon(tint = MaterialTheme.colorScheme.primary, size = 18.dp) },
            label = stringResource(id = R.string.break_label),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            onClick = onBreakClick,
            modifier = Modifier.weight(1f)
        )
    }

    if (!isRunning) {
        // Strict Mode (Phase 13): visible but disabled - never removed from the UI - see
        // TimerSecondaryStopButton's own doc comment.
        TimerSecondaryStopButton(onClick = onStopClick, enabled = !strictModeEnabled)
    }
}

/** A half-width sibling of TimerPrimaryButton (same shape/height, no baked-in top padding of its
 * own since two of these sit side by side in one Row) - the Running Focus screen's Pause/Resume
 * and Break buttons. */
@Composable
private fun FocusHalfButton(
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
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
private fun StopwatchModeContent(
    snapshot: TimerSessionSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    focusOverlay: FocusOverlayState? = null,
    onBreakButtonClick: () -> Unit = {},
    onStopFocusButtonClick: () -> Unit = {},
    onEndBreakButtonClick: () -> Unit = {},
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val hapticTick = rememberHapticTick()
    val isRunning = snapshot.isStopwatchRunning
    val isPaused = snapshot.isStopwatchPaused
    val isActive = isRunning || isPaused
    val isOnBreak = focusOverlay?.isOnBreak == true
    val elapsedMillis = when {
        isOnBreak -> focusOverlay!!.breakRemainingNowMillis()
        isActive -> snapshot.stopwatchElapsedNowMillis()
        else -> 0L
    }

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

    if (focusOverlay != null) {
        FocusRunningControls(
            isOnBreak = isOnBreak,
            isRunning = isRunning,
            breaksRemaining = focusOverlay.breaksRemaining,
            strictModeEnabled = focusOverlay.strictModeEnabled,
            onPause = { hapticTick(); onPause() },
            onResume = { hapticTick(); onResume() },
            onBreakClick = { hapticTick(); onBreakButtonClick() },
            onStopClick = { hapticTick(); onStopFocusButtonClick() },
            onEndBreakClick = { hapticTick(); onEndBreakButtonClick() },
            wallpaper = wallpaper
        )
    } else {
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

/** [enabled] defaults to true for this button's plain (non-Focus) Timer/Stopwatch call sites,
 * which have no notion of Strict Mode. The Focus session's own call site (FocusRunningControls)
 * passes `!strictModeEnabled` - per Phase 13, the Stop action must stay visible but become
 * non-interactive (dimmed, no click) while Strict Mode is active, never be removed from the UI. */
@Composable
private fun TimerSecondaryStopButton(onClick: () -> Unit, enabled: Boolean = true) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OneTaskStopIcon(tint = contentColor, size = 16.dp)
        Text(
            text = stringResource(id = R.string.timer_stop_button),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Lets the user resume Focus before the break's own 10-minute countdown elapses on its own -
 * same shape/colors/typography as [TimerSecondaryStopButton] (this screen's existing secondary-
 * action row style), just a different icon/label, so the on-break state's own established layout
 * and visual language stay unchanged. */
@Composable
private fun TimerSecondaryEndBreakButton(onClick: () -> Unit) {
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
        OneTaskPlayIcon(tint = MaterialTheme.colorScheme.primary, size = 16.dp)
        Text(
            text = stringResource(id = R.string.focus_end_break_button),
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

