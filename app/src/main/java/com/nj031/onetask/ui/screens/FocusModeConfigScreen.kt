package com.nj031.onetask.ui.screens

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
import com.nj031.onetask.ui.theme.OneTaskLeafIcon
import com.nj031.onetask.ui.theme.OneTaskLockIcon
import com.nj031.onetask.ui.theme.OneTaskPhoneIcon
import com.nj031.onetask.ui.theme.OneTaskPlayIcon
import com.nj031.onetask.ui.theme.OneTaskStopwatchIcon
import com.nj031.onetask.ui.theme.OneTaskTargetIcon

private enum class FocusModeTab { TIMER, STOPWATCH }
private enum class FocusLevel { LIGHT, DEEP, STRICT }

/**
 * Fixed accents for Block Distractions (green) and Automatic Start (purple), matching the
 * reference design's per-section color differentiation exactly - independent of the user's
 * selected app-wide color theme, since neither green nor purple is a semantic slot in One Task's
 * theme system (see Color.kt's own "Purple is not one of this app's theme options" note). Focus
 * Level and Notifications & Calls instead reuse the app's own dynamic MaterialTheme.colorScheme
 * primary/secondaryContainer tokens, matching the reference's blue in this app's default theme
 * while staying theme-adaptive like every other screen.
 */
private val BlockDistractionsAccent = Color(0xFF22B455)
private val BlockDistractionsAccentBackground = Color(0xFFE8F8EE)
private val AutomaticStartAccent = Color(0xFF7C5CFC)
private val AutomaticStartAccentBackground = Color(0xFFF1ECFF)

/** Static placeholder tile colors for the Block Distractions app-preview row - plain color
 * swatches, not real app icons/logos, since Phase 1 has no actual app selection to preview. */
private val AppPreviewColors = listOf(
    Color(0xFFFF4D4D),
    Color(0xFFD62E7A),
    Color(0xFFFF7A1A),
    Color(0xFF5865F2)
)

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
    var automaticStartEnabled by remember { mutableStateOf(true) }
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
                icon = { tint -> OneTaskTargetIcon(tint = tint, size = 18.dp) },
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                title = stringResource(id = R.string.focus_mode_config_level_title),
                subtitle = stringResource(id = R.string.focus_mode_config_level_subtitle),
                modifier = Modifier.padding(top = 28.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusLevelCard(
                    icon = { tint -> OneTaskLeafIcon(tint = tint, size = 18.dp) },
                    title = stringResource(id = R.string.focus_mode_config_level_light),
                    description = stringResource(id = R.string.focus_mode_config_level_light_description),
                    selected = selectedLevel == FocusLevel.LIGHT,
                    onClick = { hapticTick(); selectedLevel = FocusLevel.LIGHT },
                    modifier = Modifier.weight(1f)
                )
                FocusLevelCard(
                    icon = { tint -> OneTaskTargetIcon(tint = tint, size = 18.dp) },
                    title = stringResource(id = R.string.focus_mode_config_level_deep),
                    description = stringResource(id = R.string.focus_mode_config_level_deep_description),
                    selected = selectedLevel == FocusLevel.DEEP,
                    onClick = { hapticTick(); selectedLevel = FocusLevel.DEEP },
                    modifier = Modifier.weight(1f)
                )
                FocusLevelCard(
                    icon = { tint -> OneTaskLockIcon(tint = tint, size = 18.dp) },
                    title = stringResource(id = R.string.focus_mode_config_level_strict),
                    description = stringResource(id = R.string.focus_mode_config_level_strict_description),
                    selected = selectedLevel == FocusLevel.STRICT,
                    onClick = { hapticTick(); selectedLevel = FocusLevel.STRICT },
                    modifier = Modifier.weight(1f)
                )
            }

            BlockDistractionsCard(modifier = Modifier.padding(top = 24.dp))

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

/** Mirrors the reference's plain two-segment pill (no extra outer border) - the ONE functional
 * interaction in this Phase 1 screen, purely to switch which UI state below is shown; it does not
 * connect to real Timer/Stopwatch state. */
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

/** Focus Time/Breaks (Timer state) or Total duration/Breaks (Stopwatch state) - reference-style
 * value+chevron rows. Values shown are the reference's own static placeholders; real Focus
 * Time/Break logic and Stopwatch Focus logic are both out of scope for this UI-only phase. */
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
                        value = stringResource(id = R.string.focus_mode_config_focus_time_value)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    FocusModeValueRow(
                        title = stringResource(id = R.string.focus_mode_config_breaks),
                        value = stringResource(id = R.string.focus_mode_config_breaks_value_timer)
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
                        value = stringResource(id = R.string.focus_mode_config_breaks_value_stopwatch)
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
private fun FocusModeSectionLabel(
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

/** One Focus Level option (Light/Deep/Strict) - a compact, selectable reference-style card:
 * icon top-left in a neutral circle, a radio indicator top-right, title and description below.
 * UI-only: no restrictions are actually applied for whichever level is highlighted here. */
@Composable
private fun FocusLevelCard(
    icon: @Composable (tint: Color) -> Unit,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                icon(MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FocusLevelRadioIndicator(selected = selected)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** A plain radio button (outline ring, filled with a blue dot when selected) - the reference's
 * own selection indicator for a Focus Level card, deliberately not a checkmark. */
@Composable
private fun FocusLevelRadioIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/** Section icon + title/subtitle + chevron, plus a small selected-app preview row below (static
 * color-swatch placeholders standing in for real app icons - Phase 1 has no actual app selection,
 * so these are not real app logos) - reference's "app-selection preview area". UI-only: tapping
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
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppPreviewColors.forEach { color ->
                    AppPreviewTile(color = color, modifier = Modifier.padding(end = 8.dp))
                }
                MoreAppsChip(count = 3)
            }
            Text(
                text = stringResource(id = R.string.focus_mode_config_apps_selected_format, 4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AppPreviewTile(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color)
    )
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
                .background(AutomaticStartAccentBackground),
            contentAlignment = Alignment.Center
        ) {
            OneTaskPlayIcon(tint = AutomaticStartAccent, size = 18.dp)
        }
        Text(
            text = stringResource(id = R.string.focus_mode_config_automatic_start_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}
