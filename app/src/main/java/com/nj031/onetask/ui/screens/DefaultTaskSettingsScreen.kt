package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.components.durationMillisToWholeMinutes
import com.nj031.onetask.ui.haptics.rememberHapticTick

private const val TIMER_25_MIN = 25
private const val TIMER_45_MIN = 45
private const val TIMER_60_MIN = 60
private val TIMER_PRESETS = listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN)

// This screen's defaultTimerMinutes only ever stores whole minutes (see
// GeneralSettingsRepository), so the shared H/M/S OneTaskDurationPickerDialog's confirm button is
// disabled below a full minute - the same reasoning AddTaskScreen's own Timer field uses.
private const val MIN_CUSTOM_DEFAULT_TIMER_MILLIS = 60_000L

/**
 * General Settings > Default Task Settings. Seeds only the initial fields a brand-new Add Task
 * shows - AddTaskScreen still lets the user change any of these per-task before saving, and
 * changing a default here never touches a task that already exists. Deliberately mirrors
 * AddTaskScreen's own Timer/Pending Task rows (same chip style, same custom-timer input) rather
 * than inventing a new settings UI. Category management (Default display + Custom add/rename/
 * delete) used to live on this screen too - it moved to its own dedicated Categories screen (see
 * CategoriesScreen.kt), reached from General Settings' own "Categories" row, without changing any
 * of its underlying behavior or persistence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultTaskSettingsScreen(
    defaultTimerMinutes: Int?,
    defaultPostponeIfIncomplete: Boolean,
    onDefaultTimerMinutesChange: (Int?) -> Unit,
    onDefaultPostponeIfIncompleteChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    var showCustomDurationPicker by remember { mutableStateOf(false) }
    val hapticTick = rememberHapticTick()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(id = R.string.default_task_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.default_task_settings_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            DefaultSettingSectionLabel(text = stringResource(id = R.string.default_timer_section))
            val isCustomTimerSelected = defaultTimerMinutes != null && defaultTimerMinutes !in TIMER_PRESETS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultSettingChip(
                        text = stringResource(id = R.string.option_no_timer),
                        selected = !isCustomTimerSelected && defaultTimerMinutes == null,
                        onClick = { onDefaultTimerMinutesChange(null) }
                    )
                    TIMER_PRESETS.forEach { minutes ->
                        DefaultSettingChip(
                            text = minutes.toString(),
                            selected = !isCustomTimerSelected && defaultTimerMinutes == minutes,
                            onClick = { onDefaultTimerMinutesChange(minutes) }
                        )
                    }
                    DefaultSettingChip(
                        text = stringResource(id = R.string.option_custom),
                        selected = isCustomTimerSelected,
                        onClick = { showCustomDurationPicker = true }
                    )
                }
            }

            DefaultSettingSectionLabel(
                text = stringResource(id = R.string.default_pending_task_section),
                topPadding = 24.dp
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.pending_task_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = defaultPostponeIfIncomplete,
                        onCheckedChange = {
                            hapticTick()
                            onDefaultPostponeIfIncompleteChange(it)
                        }
                    )
                }
            }
        }
    }

    if (showCustomDurationPicker) {
        OneTaskDurationPickerDialog(
            initialMillis = (defaultTimerMinutes ?: 0).toLong() * 60_000L,
            minDurationMillis = MIN_CUSTOM_DEFAULT_TIMER_MILLIS,
            onDismiss = { showCustomDurationPicker = false },
            onConfirm = { millis ->
                onDefaultTimerMinutesChange(durationMillisToWholeMinutes(millis))
                showCustomDurationPicker = false
            }
        )
    }
}

@Composable
private fun DefaultSettingSectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = topPadding, bottom = 10.dp)
    )
}

/** Same pill-shaped selectable chip style AddTaskScreen's Tag/Date/Repeat/Timer rows use. */
@Composable
private fun DefaultSettingChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}
