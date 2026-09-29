package com.nj031.onetask.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nj031.onetask.ui.theme.OneTaskFocusTaskIcon
import com.nj031.onetask.ui.theme.OneTaskLapFlagIcon
import com.nj031.onetask.ui.theme.OneTaskLeafIcon
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
    onStopwatchFocusModeClick: () -> Unit = {},
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
    var showStopwatchRunningWarning by remember { mutableStateOf(false) }

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

    // Same guard as handleFocusModeClick, mirrored for the Stopwatch tab's own Focus mode entry:
    // this button only ever renders while no session (normal or Focus) is already active on the
    // Stopwatch tab, so the collision it guards against can only ever be a normal (non-Focus)
    // running/paused Stopwatch.
    val handleStopwatchFocusModeClick: () -> Unit = {
        if (snapshot.isStopwatchRunning || snapshot.isStopwatchPaused) {
            showStopwatchRunningWarning = true
        } else {
            onStopwatchFocusModeClick()
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
                wallpaper = wallpaper
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
                    wallpaper = wallpaper,
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
                        onFocusModeClick = handleStopwatchFocusModeClick,
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
            title = stringResource(id = R.string.focus_timer_running_warning_title),
            message = stringResource(id = R.string.focus_timer_running_warning_message),
            onContinue = {
                showTimerRunningWarning = false
                viewModel.stopTimer()
                onFocusModeClick()
            },
            onDismiss = { showTimerRunningWarning = false }
        )
    }

    if (showStopwatchRunningWarning) {
        TimerRunningWarningDialog(
            title = stringResource(id = R.string.focus_stopwatch_running_warning_title),
            message = stringResource(id = R.string.focus_stopwatch_running_warning_message),
            onContinue = {
                showStopwatchRunningWarning = false
                viewModel.stopStopwatch()
                onStopwatchFocusModeClick()
            },
            onDismiss = { showStopwatchRunningWarning = false }
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

/** Shared shell for both the Timer- and Stopwatch-tab "already running" collision warning (see
 * handleFocusModeClick/handleStopwatchFocusModeClick) - same dialog, just the title/message and
 * onContinue action differ per which engine is actually running. */
@Composable
private fun TimerRunningWarningDialog(title: String, message: String, onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
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

    // Deliberately transparent even while a wallpaper is active - per the Wallpaper 1 spec's own
    // "Top App Bars/Headers: transparent, wallpaper visible behind it" rule, this header sits
    // directly over the wallpaper image with no card-like surface behind it (unlike this screen's
    // own pills/segmented control, which stay opaque per the spec's separate "Cards are opaque"
    // rule - a header isn't a Card).
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.timer_focus_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                // primary (green) reads poorly directly against the wallpaper's blue background -
                // onBackground is already tuned for text sitting directly on the wallpaper (see
                // Theme.kt) rather than on an opaque Card. Non-wallpaper themes keep their
                // original accent-colored title exactly as before.
                color = if (wallpaper != Wallpaper.NONE) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.timer_focus_subtitle),
                style = MaterialTheme.typography.bodySmall,
                // onSurfaceVariant (chosen for text on an opaque Card) also reads poorly directly
                // on the wallpaper background - a dimmed onBackground preserves this subtitle's
                // lighter emphasis relative to the title above without losing readability. Non-
                // wallpaper themes are unaffected.
                color = if (wallpaper != Wallpaper.NONE) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box {
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
    wallpaper: Wallpaper = Wallpaper.NONE,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SegmentedTab(
            icon = { tint -> OneTaskClockIcon(tint = tint, size = 18.dp) },
            label = stringResource(id = R.string.timer_tab_timer),
            selected = selectedTab == TimerTab.TIMER,
            enabled = !timerLocked,
            onClick = onSelectTimer,
            wallpaper = wallpaper,
            modifier = Modifier.weight(1f)
        )
        SegmentedTab(
            icon = { tint -> OneTaskStopwatchIcon(tint = tint, size = 18.dp) },
            label = stringResource(id = R.string.timer_tab_stopwatch),
            selected = selectedTab == TimerTab.STOPWATCH,
            enabled = !stopwatchLocked,
            onClick = onSelectStopwatch,
            wallpaper = wallpaper,
            modifier = Modifier.weight(1f)
        )
    }
}

/** A light icon+label tab with a small dot underneath marking the active mode, replacing the
 * segmented control's old heavy bordered/filled-pill look - same selection/lock behavior as
 * before, purely restyled. */
@Composable
private fun SegmentedTab(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE,
    modifier: Modifier = Modifier
) {
    val tint = when {
        selected -> MaterialTheme.colorScheme.primary
        !enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // The icon keeps its existing tint (above) unconditionally - only the label text color
    // changes for Wallpaper 1, since this control sits directly on the wallpaper image (no Card
    // background) and primary/onSurfaceVariant (tuned for an opaque Card) read poorly there. See
    // TimerTopBar's title/subtitle fix just above for the same onBackground-based approach.
    val labelColor = if (wallpaper != Wallpaper.NONE) {
        val onBackground = MaterialTheme.colorScheme.onBackground
        if (selected) onBackground else onBackground.copy(alpha = 0.6f)
    } else {
        tint
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 14.dp, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
            wallpaper = wallpaper,
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
            !isActive -> TimerPrimaryControlRow(
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPlayIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.timer_start_focus_session_button),
                        onClick = { hapticTick(); onStart() },
                        wallpaper = wallpaper
                    )
                }
            )
            isRunning -> TimerPrimaryControlRow(
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPauseIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.pause),
                        onClick = { hapticTick(); onPause() },
                        wallpaper = wallpaper
                    )
                }
            )
            isPaused -> TimerPrimaryControlRow(
                leftSlot = {
                    TimerSlotButton(
                        icon = { tint -> OneTaskStopIcon(tint = tint, size = 18.dp) },
                        label = stringResource(id = R.string.timer_stop_button),
                        onClick = { hapticTick(); onStop() }
                    )
                },
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPlayIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.resume),
                        onClick = { hapticTick(); onResume() },
                        wallpaper = wallpaper
                    )
                }
            )
        }

        TimerSecondaryActionsRow(
            onFocusModeClick = onFocusModeClick,
            focusModeEnabled = !isRunning
        )
    }
}

/** The Running Focus screen's own bottom controls: Stop | Pause/Resume | Break while
 * running/paused (Stop stays visible, disabled instead of hidden, while Strict Mode is on), or a
 * plain "on break" indicator - no buttons at all, since a break always resumes automatically -
 * while a manual break is counting down. */
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
        // Same full-width secondary-action row style this on-break state already used - the
        // minimum addition needed to let the user resume Focus before the break's own 10-minute
        // countdown elapses on its own.
        TimerSecondaryEndBreakButton(onClick = onEndBreakClick)
        return
    }

    // Stop stays visible (disabled while Strict Mode is on) in both running and paused states,
    // positioned left of the primary control - never removed from the UI, per Strict Mode's own
    // "visible but non-interactive" rule (see TimerSlotButton's enabled param).
    TimerPrimaryControlRow(
        leftSlot = {
            TimerSlotButton(
                icon = { tint -> OneTaskStopIcon(tint = tint, size = 18.dp) },
                label = stringResource(id = R.string.timer_stop_button),
                onClick = onStopClick,
                enabled = !strictModeEnabled
            )
        },
        rightSlot = {
            TimerSlotButton(
                icon = { tint -> OneTaskCoffeeCupIcon(tint = tint, size = 18.dp) },
                label = stringResource(id = R.string.break_label),
                onClick = onBreakClick
            )
        },
        primary = {
            if (isRunning) {
                TimerCircularPrimaryButton(
                    icon = { OneTaskPauseIcon(tint = Color.White, size = 26.dp) },
                    label = stringResource(id = R.string.pause),
                    onClick = onPause,
                    wallpaper = wallpaper
                )
            } else {
                TimerCircularPrimaryButton(
                    icon = { OneTaskPlayIcon(tint = Color.White, size = 26.dp) },
                    label = stringResource(id = R.string.resume),
                    onClick = onResume,
                    wallpaper = wallpaper
                )
            }
        }
    )
}

@Composable
private fun StopwatchModeContent(
    snapshot: TimerSessionSnapshot,
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
            !isActive -> TimerPrimaryControlRow(
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPlayIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.timer_start_focus_session_button),
                        onClick = { hapticTick(); onStart() },
                        wallpaper = wallpaper
                    )
                }
            )
            isRunning -> TimerPrimaryControlRow(
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPauseIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.pause),
                        onClick = { hapticTick(); onPause() },
                        wallpaper = wallpaper
                    )
                }
            )
            isPaused -> TimerPrimaryControlRow(
                leftSlot = {
                    TimerSlotButton(
                        icon = { tint -> OneTaskStopIcon(tint = tint, size = 18.dp) },
                        label = stringResource(id = R.string.timer_stop_button),
                        onClick = { hapticTick(); onStop() }
                    )
                },
                primary = {
                    TimerCircularPrimaryButton(
                        icon = { OneTaskPlayIcon(tint = Color.White, size = 26.dp) },
                        label = stringResource(id = R.string.resume),
                        onClick = { hapticTick(); onResume() },
                        wallpaper = wallpaper
                    )
                }
            )
        }

        TimerSecondaryActionsRow(
            onFocusModeClick = onFocusModeClick,
            focusModeEnabled = !isRunning
        )
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
 * Circular (matching the ring it backs) rather than the cards' rounded-rect, reusing the same
 * theme border/surface tokens every Card already reads - not a new color, and the ring/label
 * themselves are unchanged.
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
    val trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    val progressColor = MaterialTheme.colorScheme.primary
    val clamped = remainingFraction.coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidthPx = 6.dp.toPx()
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
            // onSurface (not onBackground): this label sits inside timerRingBackingModifier's own
            // opaque Card-like circle while a wallpaper is active (a plain, unfilled 280dp box
            // otherwise, where onSurface and onBackground are identical anyway).
            color = MaterialTheme.colorScheme.onSurface
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
    wallpaper: Wallpaper = Wallpaper.NONE,
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
                wallpaper = wallpaper,
                modifier = Modifier.weight(1f)
            )
        }
        TimerPresetPill(
            label = stringResource(id = R.string.timer_preset_custom),
            selected = isCustomSelected,
            onClick = onCustomClick,
            wallpaper = wallpaper,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimerPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE,
    modifier: Modifier = Modifier
) {
    val hapticTick = rememberHapticTick()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50)
            )
            .clickable { hapticTick(); onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                // Selected pills sit on an opaque secondaryContainer background (cream) in every
                // theme including Wallpaper 1 - primary already reads fine there, unchanged.
                selected -> MaterialTheme.colorScheme.primary
                // Unselected pills have a transparent fill, so on Wallpaper 1 this label sits
                // directly on the wallpaper image - onSurfaceVariant (tuned for an opaque Card)
                // reads poorly there; a dimmed onBackground preserves the same lighter emphasis.
                wallpaper != Wallpaper.NONE -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}

/** Keeps the large circular Start/Pause/Resume control visually centered no matter which of the
 * optional side slots (Stop/Break) are present for the current state - the minimum structure
 * needed to reposition those controls beside the primary one instead of stacking them below it,
 * without hardcoding which combination of slots each state uses. */
@Composable
private fun TimerPrimaryControlRow(
    primary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leftSlot: (@Composable () -> Unit)? = null,
    rightSlot: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { leftSlot?.invoke() }
        Box(contentAlignment = Alignment.Center) { primary() }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { rightSlot?.invoke() }
    }
}

/** The large circular Start/Pause/Resume control - the redesign's replacement for the old
 * full-width rectangular button, with its label shown below rather than inline. Colored via the
 * existing theme/accent system exactly as the old button was (primary/onPrimary), never a
 * hardcoded color. */
@Composable
private fun TimerCircularPrimaryButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                // Only while a wallpaper is active: same defined-edge treatment the old CTA used
                // against wallpaper pixels sitting behind it. Non-wallpaper themes are unaffected.
                .then(
                    if (wallpaper != Wallpaper.NONE) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { icon() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            // onSurfaceVariant (tuned for an opaque Card) reads poorly directly on the wallpaper
            // background this label actually sits on - onBackground is already tuned for that.
            // Non-wallpaper themes are unaffected.
            color = if (wallpaper != Wallpaper.NONE) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** A small circular icon+label control for [TimerPrimaryControlRow]'s side slots (Stop/Break) -
 * the restyled, repositioned replacement for the old full-width secondary button row. [enabled]
 * defaults to true for call sites with no notion of Strict Mode; the Focus session's own call
 * site passes `!strictModeEnabled` so Stop stays visible but non-interactive while Strict Mode is
 * active, per Phase 13 - never removed from the UI. */
@Composable
private fun TimerSlotButton(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.5f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(containerColor)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) { icon(tint) }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Lets the user resume Focus before the break's own 10-minute countdown elapses on its own -
 * same full-width secondary-action row shape/colors/typography this on-break state already used,
 * just a different icon/label, so the on-break state's own established layout and visual language
 * stay unchanged. */
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

/** The lightweight "Focus on a task" / "Focus mode" secondary row - two compact, equal-width
 * controls that never compete visually with the timer or the primary Start/Pause/Resume control.
 * "Focus on a task" is an explicit visual placeholder only (per the redesign spec): it has no
 * click handler and renders permanently non-interactive, since no task-selection flow exists for
 * it yet. "Focus mode" opens the real, unchanged Focus Mode Configuration screen (UI-only Phase 1
 * - see FocusModeConfigScreen) - [focusModeEnabled] is false only while the Timer/Stopwatch is
 * actively running, so the control stays visible but non-clickable rather than disappearing. */
@Composable
private fun TimerSecondaryActionsRow(
    onFocusModeClick: () -> Unit,
    focusModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val hapticTick = rememberHapticTick()
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactSecondaryAction(
            icon = { tint -> OneTaskFocusTaskIcon(tint = tint, size = 18.dp) },
            label = stringResource(id = R.string.timer_focus_on_task_button),
            onClick = {},
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        CompactSecondaryAction(
            icon = { tint -> OneTaskLeafIcon(tint = tint, size = 18.dp) },
            label = stringResource(id = R.string.timer_focus_mode_button),
            onClick = { hapticTick(); onFocusModeClick() },
            enabled = focusModeEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactSecondaryAction(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.6f else 0.3f), RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

