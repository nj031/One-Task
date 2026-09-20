package com.nj031.onetask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.task.CategoryEntity
import com.nj031.onetask.data.task.DefaultCategory
import com.nj031.onetask.ui.components.OneTaskDurationPickerDialog
import com.nj031.onetask.ui.components.categoryDisplayName
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
 * AddTaskScreen's own Timer/Tag/Pending Task rows (same chip style, same custom-timer input)
 * rather than inventing a new settings UI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultTaskSettingsScreen(
    defaultTimerMinutes: Int?,
    defaultPostponeIfIncomplete: Boolean,
    customCategories: List<CategoryEntity>,
    onDefaultTimerMinutesChange: (Int?) -> Unit,
    onDefaultPostponeIfIncompleteChange: (Boolean) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onRenameCustomCategory: (id: String, name: String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var showCustomDurationPicker by remember { mutableStateOf(false) }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var categoryPendingRename by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryPendingDeletion by remember { mutableStateOf<CategoryEntity?>(null) }
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
            FlowRow(
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

            // The 5 fixed Categories every account has (see DefaultCategory) - display-only here,
            // unlike the old Default Tag chips this replaces: Category is never auto-assigned to
            // a new task (see the Category spec), so there's no "pick which one seeds new tasks"
            // selection to make.
            DefaultSettingSectionLabel(
                text = stringResource(id = R.string.default_category_section),
                topPadding = 24.dp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DefaultCategory.values().forEach { category ->
                    DefaultSettingChip(
                        text = categoryDisplayName(category.id, emptyList()),
                        selected = false,
                        onClick = {}
                    )
                }
            }

            // Default Categories (above) can never be renamed or deleted. Custom Categories
            // (below) are user-created, permanently persisted (Room, not in-memory), and can be
            // renamed (tap the name) or deleted (see CustomCategoryRow) - both managed only from
            // this screen, never from Add/Edit Task directly, per the Category spec.
            DefaultSettingSectionLabel(
                text = stringResource(id = R.string.custom_categories_section),
                topPadding = 24.dp
            )
            Column {
                customCategories.forEach { category ->
                    CustomCategoryRow(
                        category = category,
                        onRenameClick = { categoryPendingRename = category },
                        onDeleteClick = { categoryPendingDeletion = category }
                    )
                }
                AddCustomCategoryButton(onClick = { showAddCustomCategoryDialog = true })
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
                    onCheckedChange = {
                        hapticTick()
                        onDefaultPostponeIfIncompleteChange(it)
                    }
                )
            }
        }
    }

    if (showAddCustomCategoryDialog) {
        AddOrRenameCustomCategoryDialog(
            titleRes = R.string.add_custom_category_button,
            initialName = "",
            confirmLabelRes = R.string.add,
            onConfirm = { name ->
                onAddCustomCategory(name)
                showAddCustomCategoryDialog = false
            },
            onDismiss = { showAddCustomCategoryDialog = false }
        )
    }

    val categoryToRename = categoryPendingRename
    if (categoryToRename != null) {
        AddOrRenameCustomCategoryDialog(
            titleRes = R.string.rename_custom_category_title,
            initialName = categoryToRename.name,
            confirmLabelRes = R.string.save,
            onConfirm = { name ->
                onRenameCustomCategory(categoryToRename.id, name)
                categoryPendingRename = null
            },
            onDismiss = { categoryPendingRename = null }
        )
    }

    val categoryToDelete = categoryPendingDeletion
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryPendingDeletion = null },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.delete_custom_category_confirm_message,
                        categoryToDelete.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticTick()
                        onDeleteCustomCategory(categoryToDelete.id)
                        categoryPendingDeletion = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDeletion = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
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

/** [onRenameClick] on the name itself (not a separate edit icon) is Settings' own affordance for
 * the Category spec's rename requirement - Add/Edit Task's Category row deliberately has no
 * equivalent, since creating/renaming/deleting is managed only from here. */
@Composable
private fun CustomCategoryRow(category: CategoryEntity, onRenameClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).clickable(onClick = onRenameClick)
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(id = R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Same small "+ label" pill style AddTaskScreen's AddChipButton (e.g. "+ Add Subtask") uses. */
@Composable
private fun AddCustomCategoryButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(id = R.string.add_custom_category_button),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/** Shared by both "Add Custom Category" and "Rename Category" - the Category spec allows
 * duplicate names (including matching a default category's name), so unlike the old Tag dialog
 * this replaces, there is no duplicate-name validation here at all. */
@Composable
private fun AddOrRenameCustomCategoryDialog(
    titleRes: Int,
    initialName: String,
    confirmLabelRes: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(id = R.string.custom_category_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotEmpty()
            ) {
                Text(text = stringResource(id = confirmLabelRes))
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
