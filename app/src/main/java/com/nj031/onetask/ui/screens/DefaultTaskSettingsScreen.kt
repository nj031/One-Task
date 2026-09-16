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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R

private const val TIMER_25_MIN = 25
private const val TIMER_45_MIN = 45
private const val TIMER_60_MIN = 60
private val TIMER_PRESETS = listOf(TIMER_25_MIN, TIMER_45_MIN, TIMER_60_MIN)

/**
 * General Settings > Default Task Settings. Seeds only the initial fields a brand-new Add Task
 * shows - AddTaskScreen still lets the user change any of these per-task before saving, and
 * changing a default here never touches a task that already exists. Deliberately mirrors
 * AddTaskScreen's own Timer/Tag/Pending Task rows (same chip style, same custom-timer input)
 * rather than inventing a new settings UI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultTaskSettingsScreen(
    defaultTimerMinutes: Int?,
    defaultTag: String?,
    defaultPostponeIfIncomplete: Boolean,
    onDefaultTimerMinutesChange: (Int?) -> Unit,
    onDefaultTagChange: (String?) -> Unit,
    onDefaultPostponeIfIncompleteChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val isInitialCustomTimer = defaultTimerMinutes != null && defaultTimerMinutes !in TIMER_PRESETS
    var showCustomTimerInput by remember { mutableStateOf(isInitialCustomTimer) }
    var customTimerText by remember {
        mutableStateOf(if (isInitialCustomTimer) defaultTimerMinutes.toString() else "")
    }

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
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultSettingChip(
                        text = stringResource(id = R.string.option_no_timer),
                        selected = !showCustomTimerInput && defaultTimerMinutes == null,
                        onClick = {
                            showCustomTimerInput = false
                            onDefaultTimerMinutesChange(null)
                        }
                    )
                    TIMER_PRESETS.forEach { minutes ->
                        DefaultSettingChip(
                            text = minutes.toString(),
                            selected = !showCustomTimerInput && defaultTimerMinutes == minutes,
                            onClick = {
                                showCustomTimerInput = false
                                onDefaultTimerMinutesChange(minutes)
                            }
                        )
                    }
                    DefaultSettingChip(
                        text = stringResource(id = R.string.option_custom),
                        selected = showCustomTimerInput,
                        onClick = { showCustomTimerInput = true }
                    )
                }
                if (showCustomTimerInput) {
                    TextField(
                        value = customTimerText,
                        onValueChange = {
                            val digitsOnly = it.filter(Char::isDigit)
                            customTimerText = digitsOnly
                            onDefaultTimerMinutesChange(digitsOnly.toIntOrNull())
                        },
                        placeholder = { Text(stringResource(id = R.string.custom_timer_minutes_hint)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            DefaultSettingSectionLabel(
                text = stringResource(id = R.string.default_tag_section),
                topPadding = 24.dp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    stringResource(id = R.string.tag_personal),
                    stringResource(id = R.string.tag_work),
                    stringResource(id = R.string.tag_study),
                    stringResource(id = R.string.tag_health)
                ).forEach { tag ->
                    DefaultSettingChip(
                        text = tag,
                        selected = tag == defaultTag,
                        onClick = { onDefaultTagChange(if (defaultTag == tag) null else tag) }
                    )
                }
            }

            DefaultSettingSectionLabel(
                text = stringResource(id = R.string.default_pending_task_section),
                topPadding = 24.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    onCheckedChange = onDefaultPostponeIfIncompleteChange
                )
            }
        }
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
