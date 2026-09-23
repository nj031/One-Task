package com.nj031.onetask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskCardViewIcon
import com.nj031.onetask.ui.theme.OneTaskClockIcon
import com.nj031.onetask.ui.theme.OneTaskMoonIcon
import com.nj031.onetask.ui.theme.OneTaskPlayIcon
import com.nj031.onetask.ui.theme.OneTaskShieldIcon
import com.nj031.onetask.ui.theme.OneTaskStopwatchIcon
import com.nj031.onetask.ui.theme.OneTaskSunIcon

private enum class FocusModeTab { TIMER, STOPWATCH }
private enum class FocusLevel { LIGHT, DEEP, STRICT }

/**
 * Phase 1 (UI-only) build of the new distraction-blocking-style "Focus mode" configuration
 * screen, reached by tapping the Timer tab's own Focus mode button (see TimerPlaceholderScreen).
 * Only the Timer/Stopwatch selector below is functional, purely so both of its reference UI
 * states are reachable - every other row/card/toggle here is a visual placeholder. App blocking,
 * notification/call muting, Focus Level restrictions, Automatic Start, real Focus Time/Break
 * durations, and the actual "Save & Start Focus" running session are all deliberately
 * unimplemented and land in a later phase. This screen is entirely separate from, and does not
 * touch, the existing task-timer Focus Mode feature (FocusTimerScreen/TimerForegroundService).
 */
@Composable
fun FocusModeConfigScreen(
    onCloseClick: () -> Unit,
    onSaveAndStartClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(FocusModeTab.TIMER) }
    var selectedLevel by remember { mutableStateOf(FocusLevel.DEEP) }
    var automaticStartEnabled by remember { mutableStateOf(false) }
    val hapticTick = rememberHapticTick()

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
                modifier = Modifier.padding(top = 16.dp)
            )

            FocusModeSectionLabel(
                title = stringResource(id = R.string.focus_mode_config_level_title),
                subtitle = stringResource(id = R.string.focus_mode_config_level_subtitle),
                modifier = Modifier.padding(top = 28.dp)
            )
            FocusLevelCard(
                icon = { tint -> OneTaskSunIcon(tint = tint, size = 22.dp) },
                title = stringResource(id = R.string.focus_mode_config_level_light),
                description = stringResource(id = R.string.focus_mode_config_level_light_description),
                selected = selectedLevel == FocusLevel.LIGHT,
                onClick = { hapticTick(); selectedLevel = FocusLevel.LIGHT }
            )
            FocusLevelCard(
                icon = { tint -> OneTaskMoonIcon(tint = tint, size = 22.dp) },
                title = stringResource(id = R.string.focus_mode_config_level_deep),
                description = stringResource(id = R.string.focus_mode_config_level_deep_description),
                selected = selectedLevel == FocusLevel.DEEP,
                onClick = { hapticTick(); selectedLevel = FocusLevel.DEEP },
                modifier = Modifier.padding(top = 10.dp)
            )
            FocusLevelCard(
                icon = { tint -> OneTaskShieldIcon(tint = tint, size = 22.dp) },
                title = stringResource(id = R.string.focus_mode_config_level_strict),
                description = stringResource(id = R.string.focus_mode_config_level_strict_description),
                selected = selectedLevel == FocusLevel.STRICT,
                onClick = { hapticTick(); selectedLevel = FocusLevel.STRICT },
                modifier = Modifier.padding(top = 10.dp)
            )

            BlockDistractionsCard(modifier = Modifier.padding(top = 28.dp))

            NotificationsCallsCard(modifier = Modifier.padding(top = 16.dp))

            AutomaticStartCard(
                enabled = automaticStartEnabled,
                onEnabledChange = { hapticTick(); automaticStartEnabled = it },
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = { hapticTick(); onSaveAndStartClick() },
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
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
                Text(
                    text = stringResource(id = R.string.focus_mode_config_save_and_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
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

/** Mirrors TimerPlaceholderScreen's own TimerStopwatchSegmentedControl visual pattern (blue
 * selected / neutral unselected) - the ONE functional interaction in this Phase 1 screen, purely
 * to switch which UI state below is shown; it does not connect to real Timer/Stopwatch state. */
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
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
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
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
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

/** Focus Time/Breaks (Timer state) or Total duration/Breaks (Stopwatch state) - reference-style
 * value+chevron rows. Values shown are static placeholders; real Focus Time/Break logic and
 * Stopwatch Focus logic are both out of scope for this UI-only phase. */
@Composable
private fun FocusModeDurationCard(selectedTab: FocusModeTab, modifier: Modifier = Modifier) {
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
                        value = stringResource(id = R.string.timer_minutes_format, 25)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_breaks),
                        value = stringResource(id = R.string.timer_minutes_format, 5)
                    )
                }
                FocusModeTab.STOPWATCH -> {
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_total_duration),
                        value = stringResource(id = R.string.timer_minutes_format, 60)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_breaks),
                        value = stringResource(id = R.string.timer_minutes_format, 5)
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusModeValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
private fun FocusModeSectionLabel(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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

/** One Focus Level option (Light/Deep/Strict) - a selectable, reference-style card with icon,
 * title, description and a trailing checkmark when selected. UI-only: no restrictions are
 * actually applied for whichever level is highlighted here in this phase. */
@Composable
private fun FocusLevelCard(
    icon: @Composable (tint: Color) -> Unit,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            icon(tint)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Section icon + title/subtitle + chevron, plus a small selected-app preview row (placeholder
 * circles + a count string) below - reference's "app-selection preview area". UI-only: tapping
 * this card does nothing yet, and no app-blocking permission is requested in this phase. */
@Composable
private fun BlockDistractionsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = {}),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    OneTaskCardViewIcon(tint = MaterialTheme.colorScheme.primary, size = 20.dp)
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
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
                Text(
                    text = stringResource(id = R.string.focus_mode_config_no_apps_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationsCallsCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = {})
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
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = stringResource(id = R.string.focus_mode_config_notifications_calls_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.focus_mode_config_notifications_calls_subtitle),
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

/** Toggle is visually present and locally togglable, but per spec does not implement any actual
 * automatic-start behavior in this phase. */
@Composable
private fun AutomaticStartCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
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
            OneTaskPlayIcon(tint = MaterialTheme.colorScheme.primary, size = 18.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = stringResource(id = R.string.focus_mode_config_automatic_start_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.focus_mode_config_automatic_start_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}
