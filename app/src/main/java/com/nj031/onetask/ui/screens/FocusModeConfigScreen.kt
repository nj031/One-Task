package com.nj031.onetask.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.timer.FocusCallsMode
import com.nj031.onetask.data.timer.FocusNotificationsMode
import com.nj031.onetask.data.timer.MAX_FOCUS_BREAKS
import com.nj031.onetask.service.FocusAccessibilityUtil
import com.nj031.onetask.service.FocusNotificationPolicyManager
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskCardViewIcon
import com.nj031.onetask.ui.theme.OneTaskClockIcon
import com.nj031.onetask.ui.theme.OneTaskLockIcon
import com.nj031.onetask.ui.theme.OneTaskPhoneIcon
import com.nj031.onetask.ui.theme.OneTaskStopwatchIcon
import com.nj031.onetask.viewmodel.FocusBlockedAppsViewModel
import com.nj031.onetask.viewmodel.TimerViewModel

private enum class FocusModeTab { TIMER, STOPWATCH }

/** The Timer tab's own existing Focus Time presets, reused as-is (see TimerPlaceholderScreen's
 * own TIMER_PRESET_MINUTES) rather than inventing separate Focus Mode values. */
private val FOCUS_TIME_PRESET_MINUTES = listOf(5, 25, 45, 60)
private const val DEFAULT_FOCUS_TIME_MINUTES = 5

/**
 * Fixed accent for Block Distractions (green), matching the reference design's per-section color
 * differentiation - independent of the user's selected app-wide color theme, since green is not a
 * semantic slot in One Task's theme system. Strict Mode and Notifications & Calls instead reuse
 * the app's own dynamic MaterialTheme.colorScheme primary/secondaryContainer tokens, matching the
 * reference's blue in this app's default theme while staying theme-adaptive like every other
 * screen.
 */
private val BlockDistractionsAccent = Color(0xFF22B455)
private val BlockDistractionsAccentBackground = Color(0xFFE8F8EE)

/**
 * The distraction-blocking-style "Focus mode" configuration screen, reached by tapping the Timer
 * tab's own Focus mode button (see TimerPlaceholderScreen). Fully functional: the Timer/Stopwatch
 * selector, Focus Time selection for Timer (presets + the existing shared Custom Duration picker)
 * or the fixed "0 -> infinity" for Stopwatch (no duration - it counts up indefinitely), Breaks
 * count (shared by both modes), Strict Mode (Phase 13 - see [StrictModeCard]), Block Distractions,
 * and Notifications & Calls (Phase 12 - see [NotificationsCallsCard]/[FocusNotificationPolicyManager]),
 * and Save & Start Focus, which starts the matching kind of session
 * (TimerViewModel.startFocusSession or startStopwatchFocusSession) with all of the above on the
 * shared TimerViewModel and returns to the Timer tab - see NavGraph's viewModelStoreOwner wiring
 * for why this screen shares that instance rather than getting its own.
 *
 * This screen is entirely separate from, and does not touch, the existing task-timer Focus Mode
 * feature (FocusTimerScreen/TimerForegroundService).
 */
@Composable
fun FocusModeConfigScreen(
    viewModel: TimerViewModel = viewModel(),
    blockedAppsViewModel: FocusBlockedAppsViewModel = viewModel(),
    onCloseClick: () -> Unit,
    onBlockDistractionsClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(FocusModeTab.TIMER) }
    var selectedFocusTimeMinutes by remember { mutableStateOf(DEFAULT_FOCUS_TIME_MINUTES) }
    var selectedBreaksCount by remember { mutableStateOf(MAX_FOCUS_BREAKS) }
    var selectedNotificationsMode by remember { mutableStateOf(FocusNotificationsMode.ALLOW) }
    var selectedCallsMode by remember { mutableStateOf(FocusCallsMode.ALLOW) }
    var strictModeEnabled by remember { mutableStateOf(false) }
    var showFocusTimePicker by remember { mutableStateOf(false) }
    var showCustomDurationPicker by remember { mutableStateOf(false) }
    var showBreaksPicker by remember { mutableStateOf(false) }
    var showNotificationsCallsPicker by remember { mutableStateOf(false) }
    var showEnableBlockingDialog by remember { mutableStateOf(false) }
    var showEnableNotificationPolicyDialog by remember { mutableStateOf(false) }
    val hapticTick = rememberHapticTick()
    val context = LocalContext.current

    /** Starts whichever Focus session is currently selected - the actual "Save & Start Focus"
     * action, factored out so both the direct (permissions already granted/not needed) path and
     * the post-dialog-Allow path (user returns from Settings and taps the button again) call the
     * exact same logic, never a second copy of it. */
    fun startSelectedFocusSession() {
        val blockedPackages = blockedAppsViewModel.selectedPackages.value
        when (selectedTab) {
            FocusModeTab.TIMER -> viewModel.startFocusSession(
                selectedFocusTimeMinutes * 60_000L,
                selectedBreaksCount,
                blockedPackages,
                selectedNotificationsMode,
                selectedCallsMode,
                strictModeEnabled
            )
            FocusModeTab.STOPWATCH -> viewModel.startStopwatchFocusSession(
                selectedBreaksCount,
                blockedPackages,
                selectedNotificationsMode,
                selectedCallsMode,
                strictModeEnabled
            )
        }
        onCloseClick()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            FocusModeHeader(onCloseClick = onCloseClick)

            FocusModeSegmentedControl(
                selectedTab = selectedTab,
                onSelectTimer = { hapticTick(); selectedTab = FocusModeTab.TIMER },
                onSelectStopwatch = { hapticTick(); selectedTab = FocusModeTab.STOPWATCH },
                modifier = Modifier.padding(top = 20.dp)
            )

            FocusModeDurationCard(
                selectedTab = selectedTab,
                selectedFocusTimeMinutes = selectedFocusTimeMinutes,
                selectedBreaksCount = selectedBreaksCount,
                onFocusTimeClick = { hapticTick(); showFocusTimePicker = true },
                onBreaksClick = { hapticTick(); showBreaksPicker = true },
                modifier = Modifier.padding(top = 16.dp)
            )

            StrictModeCard(
                enabled = strictModeEnabled,
                onToggle = { hapticTick(); strictModeEnabled = !strictModeEnabled },
                modifier = Modifier.padding(top = 24.dp)
            )

            BlockDistractionsCard(
                blockedAppsViewModel = blockedAppsViewModel,
                onClick = onBlockDistractionsClick,
                modifier = Modifier.padding(top = 16.dp)
            )

            NotificationsCallsCard(
                notificationsMode = selectedNotificationsMode,
                callsMode = selectedCallsMode,
                onClick = { hapticTick(); showNotificationsCallsPicker = true },
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = {
                    hapticTick()
                    val blockedPackages = blockedAppsViewModel.selectedPackages.value
                    val blockingNeedsAccessibility = blockedPackages.isNotEmpty() &&
                        !FocusAccessibilityUtil.isFocusBlockingServiceEnabled(context)
                    val notificationsCallsNeedPolicyAccess =
                        (selectedNotificationsMode != FocusNotificationsMode.ALLOW || selectedCallsMode != FocusCallsMode.ALLOW) &&
                            !FocusNotificationPolicyManager.isNotificationPolicyAccessGranted(context)
                    when {
                        blockingNeedsAccessibility -> showEnableBlockingDialog = true
                        notificationsCallsNeedPolicyAccess -> showEnableNotificationPolicyDialog = true
                        else -> startSelectedFocusSession()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.focus_mode_config_save_and_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    if (showFocusTimePicker) {
        FocusTimePickerDialog(
            selectedMinutes = selectedFocusTimeMinutes,
            onSelectPreset = { minutes ->
                selectedFocusTimeMinutes = minutes
                showFocusTimePicker = false
            },
            onOpenCustomPicker = {
                showFocusTimePicker = false
                showCustomDurationPicker = true
            },
            onDismiss = { showFocusTimePicker = false }
        )
    }

    if (showCustomDurationPicker) {
        OneTaskDurationPickerDialog(
            initialMillis = selectedFocusTimeMinutes * 60_000L,
            onDismiss = { showCustomDurationPicker = false },
            onConfirm = { millis ->
                selectedFocusTimeMinutes = (millis / 60_000L).toInt().coerceAtLeast(1)
                showCustomDurationPicker = false
            }
        )
    }

    if (showBreaksPicker) {
        BreaksPickerDialog(
            selectedCount = selectedBreaksCount,
            onConfirm = { count ->
                selectedBreaksCount = count
                showBreaksPicker = false
            },
            onDismiss = { showBreaksPicker = false }
        )
    }

    if (showEnableBlockingDialog) {
        EnableAppBlockingDialog(
            onAllow = {
                showEnableBlockingDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDeny = { showEnableBlockingDialog = false }
        )
    }

    if (showNotificationsCallsPicker) {
        NotificationsCallsPickerDialog(
            selectedNotificationsMode = selectedNotificationsMode,
            selectedCallsMode = selectedCallsMode,
            onConfirm = { notificationsMode, callsMode ->
                selectedNotificationsMode = notificationsMode
                selectedCallsMode = callsMode
                showNotificationsCallsPicker = false
            },
            onDismiss = { showNotificationsCallsPicker = false }
        )
    }

    if (showEnableNotificationPolicyDialog) {
        EnableNotificationPolicyDialog(
            onAllow = {
                showEnableNotificationPolicyDialog = false
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            },
            onDeny = { showEnableNotificationPolicyDialog = false }
        )
    }
}

/** Top-left X + centered title, deliberately with no Focus Mode ON/OFF toggle (per spec). */
@Composable
private fun FocusModeHeader(onCloseClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.focus_mode_config_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onCloseClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.focus_mode_config_close),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/** Mirrors the reference's plain two-segment pill (no extra outer border). Switches which UI
 * state below is shown; it does not connect to real Timer/Stopwatch state - only Save & Start
 * Focus (Timer only, this phase) actually starts anything. */
@Composable
private fun FocusModeSegmentedControl(
    selectedTab: FocusModeTab,
    onSelectTimer: () -> Unit,
    onSelectStopwatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        FocusModeSegmentedTab(
            icon = { tint -> OneTaskClockIcon(tint = tint, size = 20.dp) },
            label = stringResource(id = R.string.timer_tab_timer),
            selected = selectedTab == FocusModeTab.TIMER,
            onClick = onSelectTimer,
            modifier = Modifier.weight(1f)
        )
        FocusModeSegmentedTab(
            icon = { tint -> OneTaskStopwatchIcon(tint = tint, size = 20.dp) },
            label = stringResource(id = R.string.timer_tab_stopwatch),
            selected = selectedTab == FocusModeTab.STOPWATCH,
            onClick = onSelectStopwatch,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FocusModeSegmentedTab(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Focus Time/Breaks (Timer state) or Total duration/Breaks (Stopwatch state). Breaks is
 * functional in both states (shared selectedBreaksCount/onBreaksClick - one Focus session config
 * regardless of which tab starts it); Focus Time is Timer-only, and Stopwatch's Total duration
 * stays the reference's own fixed "0 -> infinity" text (never a real duration, per spec). */
@Composable
private fun FocusModeDurationCard(
    selectedTab: FocusModeTab,
    selectedFocusTimeMinutes: Int,
    selectedBreaksCount: Int,
    onFocusTimeClick: () -> Unit,
    onBreaksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breaksValue = stringResource(id = R.string.focus_mode_config_breaks_count_format, selectedBreaksCount)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            when (selectedTab) {
                FocusModeTab.TIMER -> {
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_focus_time),
                        value = stringResource(id = R.string.timer_minutes_format, selectedFocusTimeMinutes),
                        onClick = onFocusTimeClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_breaks),
                        value = breaksValue,
                        onClick = onBreaksClick
                    )
                }
                FocusModeTab.STOPWATCH -> {
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_total_duration),
                        value = stringResource(id = R.string.focus_mode_config_total_duration_value)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_breaks),
                        value = breaksValue,
                        onClick = onBreaksClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusModeValueRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FocusModeSectionLabel(
    icon: @Composable (tint: Color) -> Unit,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            icon(iconTint)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Phase 13: the single Strict Mode setting that replaced the old 3-card Focus Level concept -
 * a plain icon/title/subtitle row (matching this screen's other cards) plus a Switch, mirroring
 * NotificationsSettingsScreen's own NotificationToggleRow shape. Strict Mode only ever gates
 * whether the Stop action reaches TimerViewModel.stopFocusSession() early - see that function's
 * own doc comment and TimerPlaceholderScreen's gating of its two call sites. */
@Composable
private fun StrictModeCard(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            OneTaskLockIcon(tint = MaterialTheme.colorScheme.primary, size = 18.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = stringResource(id = R.string.focus_mode_config_strict_mode_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.focus_mode_config_strict_mode_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

/** Section icon + title/subtitle + chevron, plus a small selected-app preview row below - real
 * icons (via [AppIconImage]) for whichever installed apps are currently selected in the Block
 * Distracting Apps picker (see [FocusBlockedAppsViewModel]), not placeholder swatches. Tapping
 * this card opens that picker; no app-blocking permission is requested or enforced in this phase,
 * only the selection itself is real. */
@Composable
private fun BlockDistractionsCard(
    blockedAppsViewModel: FocusBlockedAppsViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distractingApps by blockedAppsViewModel.distractingApps.collectAsState()
    val discordApp by blockedAppsViewModel.discordApp.collectAsState()
    val telegramApp by blockedAppsViewModel.telegramApp.collectAsState()
    val otherApps by blockedAppsViewModel.otherApps.collectAsState()
    val selectedPackages by blockedAppsViewModel.selectedPackages.collectAsState()

    val selectedApps = remember(distractingApps, discordApp, telegramApp, otherApps, selectedPackages) {
        (distractingApps + listOfNotNull(discordApp, telegramApp) + otherApps)
            .filter { it.packageName in selectedPackages }
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BlockDistractionsAccentBackground),
                    contentAlignment = Alignment.Center
                ) {
                    OneTaskCardViewIcon(tint = BlockDistractionsAccent, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = stringResource(id = R.string.focus_mode_config_block_distractions_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.focus_mode_config_block_distractions_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectedApps.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    selectedApps.take(4).forEach { app ->
                        AppIconImage(app = app, size = 32.dp, modifier = Modifier.padding(end = 8.dp))
                    }
                    if (selectedApps.size > 4) {
                        MoreAppsChip(count = selectedApps.size - 4)
                    }
                }
            }
            Text(
                text = stringResource(id = R.string.focus_mode_config_apps_selected_format, selectedPackages.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MoreAppsChip(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.focus_mode_config_more_apps_format, count),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Phase 12: label shown for the currently selected Notifications mode - see
 * FocusNotificationsMode/FocusNotificationPolicyManager for how each mode actually behaves. */
@Composable
private fun FocusNotificationsMode.label(): String = when (this) {
    FocusNotificationsMode.NONE -> stringResource(id = R.string.focus_notifications_option_none)
    FocusNotificationsMode.SILENT -> stringResource(id = R.string.focus_notifications_option_silent)
    FocusNotificationsMode.ALLOW -> stringResource(id = R.string.focus_notifications_option_allow)
}

/** Phase 12: label shown for the currently selected Calls mode - see FocusCallsMode for why there
 * is deliberately no "block calls" option. */
@Composable
private fun FocusCallsMode.label(): String = when (this) {
    FocusCallsMode.SILENT -> stringResource(id = R.string.focus_calls_option_silent)
    FocusCallsMode.ALLOW -> stringResource(id = R.string.focus_calls_option_allow)
}

@Composable
private fun NotificationsCallsCard(
    notificationsMode: FocusNotificationsMode,
    callsMode: FocusCallsMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                OneTaskPhoneIcon(
                    tint = MaterialTheme.colorScheme.primary,
                    size = 16.dp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = stringResource(id = R.string.focus_mode_config_notifications_calls_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (notificationsMode == FocusNotificationsMode.ALLOW && callsMode == FocusCallsMode.ALLOW) {
                    stringResource(id = R.string.focus_mode_config_notifications_calls_subtitle)
                } else {
                    stringResource(
                        id = R.string.focus_mode_config_notifications_calls_summary_format,
                        notificationsMode.label(),
                        callsMode.label()
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Notifications &amp; Calls picker (Phase 12): two labeled option groups (Notifications' 3 modes,
 * Calls' 2 modes), each a column of selectable rows matching this screen's existing dialog shell
 * (compare BreaksPickerDialog/FocusTimePickerDialog) - a simple checkmark-style row rather than
 * pills/cards since there are more options here than fit comfortably in a row. Confirms both
 * selections together via a single Done button, matching block_apps_done_format's "Done" style
 * rather than auto-closing on tap, so the user can freely change either group before confirming. */
@Composable
private fun NotificationsCallsPickerDialog(
    selectedNotificationsMode: FocusNotificationsMode,
    selectedCallsMode: FocusCallsMode,
    onConfirm: (FocusNotificationsMode, FocusCallsMode) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingNotificationsMode by remember(selectedNotificationsMode) { mutableStateOf(selectedNotificationsMode) }
    var pendingCallsMode by remember(selectedCallsMode) { mutableStateOf(selectedCallsMode) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.focus_mode_config_notifications_calls_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = stringResource(id = R.string.focus_notifications_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NotificationsCallsOptionRow(
                    label = stringResource(id = R.string.focus_notifications_option_none),
                    description = stringResource(id = R.string.focus_notifications_option_none_description),
                    selected = pendingNotificationsMode == FocusNotificationsMode.NONE,
                    onClick = { pendingNotificationsMode = FocusNotificationsMode.NONE }
                )
                NotificationsCallsOptionRow(
                    label = stringResource(id = R.string.focus_notifications_option_silent),
                    description = stringResource(id = R.string.focus_notifications_option_silent_description),
                    selected = pendingNotificationsMode == FocusNotificationsMode.SILENT,
                    onClick = { pendingNotificationsMode = FocusNotificationsMode.SILENT }
                )
                NotificationsCallsOptionRow(
                    label = stringResource(id = R.string.focus_notifications_option_allow),
                    description = stringResource(id = R.string.focus_notifications_option_allow_description),
                    selected = pendingNotificationsMode == FocusNotificationsMode.ALLOW,
                    onClick = { pendingNotificationsMode = FocusNotificationsMode.ALLOW }
                )

                Text(
                    text = stringResource(id = R.string.focus_calls_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                NotificationsCallsOptionRow(
                    label = stringResource(id = R.string.focus_calls_option_silent),
                    description = stringResource(id = R.string.focus_calls_option_silent_description),
                    selected = pendingCallsMode == FocusCallsMode.SILENT,
                    onClick = { pendingCallsMode = FocusCallsMode.SILENT }
                )
                NotificationsCallsOptionRow(
                    label = stringResource(id = R.string.focus_calls_option_allow),
                    description = stringResource(id = R.string.focus_calls_option_allow_description),
                    selected = pendingCallsMode == FocusCallsMode.ALLOW,
                    onClick = { pendingCallsMode = FocusCallsMode.ALLOW }
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = { onConfirm(pendingNotificationsMode, pendingCallsMode) }) {
                        Text(text = stringResource(id = R.string.done_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsCallsOptionRow(label: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Basic gate dialog shown only when Notifications and/or Calls is set to a restricting mode but
 * FocusNotificationPolicyManager.isNotificationPolicyAccessGranted is currently false - mirrors
 * EnableAppBlockingDialog exactly (see its own doc comment for why this shape: deep link to system
 * Settings, no in-app runtime permission dialog exists for this either, re-checked fresh on every
 * Save & Start Focus tap rather than cached). Deny simply closes this dialog without starting
 * Focus. */
@Composable
private fun EnableNotificationPolicyDialog(onAllow: () -> Unit, onDeny: () -> Unit) {
    Dialog(onDismissRequest = onDeny) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.focus_mode_enable_notification_policy_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(id = R.string.focus_mode_enable_notification_policy_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDeny) {
                        Text(text = stringResource(id = R.string.focus_mode_enable_notification_policy_deny))
                    }
                    TextButton(onClick = onAllow) {
                        Text(text = stringResource(id = R.string.focus_mode_enable_notification_policy_allow))
                    }
                }
            }
        }
    }
}

/** Focus Time's picker: the Timer tab's own existing presets (5/25/45/60) as pills, matching
 * TimerPlaceholderScreen's TimerPresetRow/TimerPresetPill visual style, plus a Custom pill that
 * hands off to the shared OneTaskDurationPickerDialog - never a new duration-picking design. */
@Composable
private fun FocusTimePickerDialog(
    selectedMinutes: Int,
    onSelectPreset: (Int) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCustomSelected = selectedMinutes !in FOCUS_TIME_PRESET_MINUTES
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.focus_mode_config_focus_time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FOCUS_TIME_PRESET_MINUTES.forEach { minutes ->
                        FocusTimePresetPill(
                            label = stringResource(id = R.string.timer_minutes_format, minutes),
                            selected = !isCustomSelected && selectedMinutes == minutes,
                            onClick = { onSelectPreset(minutes) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    FocusTimePresetPill(
                        label = stringResource(id = R.string.timer_preset_custom),
                        selected = isCustomSelected,
                        onClick = onOpenCustomPicker,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusTimePresetPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Breaks count picker: a plain 0-[MAX_FOCUS_BREAKS] stepper - there's no existing "pick a small
 * number" control anywhere else in the app to reuse, so this is new but intentionally minimal,
 * matching the app's existing dialog shell/typography/color conventions. */
@Composable
private fun BreaksPickerDialog(selectedCount: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var pendingCount by remember(selectedCount) { mutableStateOf(selectedCount) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.focus_mode_config_breaks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    BreaksStepperButton(
                        label = "-",
                        enabled = pendingCount > 0,
                        onClick = { pendingCount = (pendingCount - 1).coerceAtLeast(0) }
                    )
                    Text(
                        text = pendingCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.widthIn(min = 40.dp),
                        textAlign = TextAlign.Center
                    )
                    BreaksStepperButton(
                        label = "+",
                        enabled = pendingCount < MAX_FOCUS_BREAKS,
                        onClick = { pendingCount = (pendingCount + 1).coerceAtMost(MAX_FOCUS_BREAKS) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = { onConfirm(pendingCount) }) {
                        Text(text = stringResource(id = R.string.timer_custom_duration_confirm))
                    }
                }
            }
        }
    }
}

/** Basic gate dialog shown only when apps are selected to block but
 * [FocusAccessibilityUtil.isFocusBlockingServiceEnabled] is currently false - see the "Save &
 * Start Focus" button's onClick. Allow deep-links to system Accessibility settings (Android has
 * no in-app runtime permission dialog for this) and does NOT itself start Focus: the user enables
 * the service there, returns, and taps Save & Start Focus again, which re-checks the service's
 * now-current state fresh (no caching - see this screen's onClick above).
 * Deny simply closes this dialog without starting Focus. Richer explanation/illustration is
 * explicitly out of scope for this phase - matches this screen's existing plain dialog shells
 * (compare BreaksPickerDialog/FocusTimePickerDialog). */
@Composable
private fun EnableAppBlockingDialog(onAllow: () -> Unit, onDeny: () -> Unit) {
    Dialog(onDismissRequest = onDeny) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.focus_mode_enable_blocking_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(id = R.string.focus_mode_enable_blocking_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDeny) {
                        Text(text = stringResource(id = R.string.focus_mode_enable_blocking_deny))
                    }
                    TextButton(onClick = onAllow) {
                        Text(text = stringResource(id = R.string.focus_mode_enable_blocking_allow))
                    }
                }
            }
        }
    }
}

@Composable
private fun BreaksStepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}
