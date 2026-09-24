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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.nj031.onetask.ui.components.categoryDisplayName
import com.nj031.onetask.ui.haptics.rememberHapticTick

/**
 * General Settings > Categories - the dedicated management screen for Task Categories, reached
 * from GeneralSettingsScreen's own "Categories" row (previously this same UI lived inside Default
 * Task Settings; only its location changed, not its behavior). Shows the 5 fixed Default
 * categories (display-only - see [DefaultCategory]) and every account-owned Custom category,
 * which can be added/renamed/deleted here. Uses the exact same CategoryEntity/TaskRepository
 * persistence Add/Edit Task's own Category selector already reads from - no new Category system,
 * no new storage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    customCategories: List<CategoryEntity>,
    onAddCustomCategory: (String) -> Unit,
    onRenameCustomCategory: (id: String, name: String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onBackClick: () -> Unit
) {
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
                    text = stringResource(id = R.string.categories_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // The 5 fixed Categories every account has (see DefaultCategory) - display-only:
            // Category is never auto-assigned to a new task (see the Category spec), so there's
            // no "pick which one seeds new tasks" selection to make, and they can never be
            // renamed or deleted.
            CategorySectionLabel(text = stringResource(id = R.string.default_category_section), topPadding = 20.dp)
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
                    DefaultCategory.values().forEach { category ->
                        CategoryChip(
                            text = categoryDisplayName(category.id, emptyList()),
                            selected = false,
                            onClick = {}
                        )
                    }
                }
            }

            // Custom Categories are user-created, permanently persisted (Room, not in-memory),
            // and can be renamed (tap the name) or deleted (see CustomCategoryRow) - both managed
            // only from this screen, never from Add/Edit Task directly, per the Category spec.
            CategorySectionLabel(
                text = stringResource(id = R.string.custom_categories_section),
                topPadding = 24.dp
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    customCategories.forEach { category ->
                        CustomCategoryRow(
                            category = category,
                            onRenameClick = { categoryPendingRename = category },
                            onDeleteClick = { categoryPendingDeletion = category }
                        )
                    }
                    AddCustomCategoryButton(onClick = { showAddCustomCategoryDialog = true })
                }
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
}

/** [onRenameClick] on the name itself (not a separate edit icon) is this screen's own affordance
 * for the Category spec's rename requirement - Add/Edit Task's Category row deliberately has no
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
 * duplicate names (including matching a default category's name), so there is no duplicate-name
 * validation here at all. */
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
private fun CategorySectionLabel(text: String, topPadding: Dp = 0.dp) {
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
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
