package com.nj031.onetask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.nj031.onetask.R
import com.nj031.onetask.data.task.CategoryEntity
import com.nj031.onetask.data.task.DefaultCategory
import com.nj031.onetask.ui.haptics.rememberHapticTick

/** [DefaultCategory] -> its display string resource. Kept in the UI layer (not the data-layer
 * enum itself) the same way every other task-domain enum in this app (TaskPriority, TaskRepeat,
 * ...) is mapped to a string resource in the composable that shows it, not in the enum. */
private fun DefaultCategory.labelRes(): Int = when (this) {
    DefaultCategory.PERSONAL -> R.string.category_personal
    DefaultCategory.WORK -> R.string.category_work
    DefaultCategory.STUDY -> R.string.category_study
    DefaultCategory.HEALTH -> R.string.category_health
    DefaultCategory.FOCUS -> R.string.category_focus
}

/** [categoryId] (a task's categoryId, or a Category filter value) -> its display name, or the
 * "No Category" string when null - the single place both the Add/Edit Task Category row and this
 * dialog's own selected-row highlight resolve a raw id into text a user actually reads. */
@Composable
fun categoryDisplayName(categoryId: String?, customCategories: List<CategoryEntity>): String {
    if (categoryId == null) return stringResource(id = R.string.category_no_category)
    DefaultCategory.fromId(categoryId)?.let { return stringResource(id = it.labelRes()) }
    return customCategories.firstOrNull { it.id == categoryId }?.name
        ?: stringResource(id = R.string.category_no_category)
}

/**
 * THE single canonical Category picker, shared as-is by both the Add/Edit Task Category section
 * (an assignment control - see AddTaskScreen) and the Tasks homepage's Category filter (a filter
 * control - see HomeScreen's TaskFilterStrip) - same visual design, same fixed ordering (No
 * Category, then a CUSTOM section, then a DEFAULT section), same "tap a row to highlight it, then
 * Choose to confirm" interaction, for both. Neither caller creates a category inline here: "+ Add
 * Category" always hands off to [onAddCategoryClick] instead (see its own doc comment).
 *
 * [currentCategoryId] is the selection this dialog opens already highlighting - null highlights
 * "No Category". [onConfirm] fires once, with whatever is highlighted at the moment "Choose" is
 * tapped (again null for "No Category"), and the dialog does not dismiss itself; the caller is
 * expected to close it from there (matching every other One Task dialog's own dismiss ownership).
 */
@Composable
fun CategorySelectorDialog(
    currentCategoryId: String?,
    customCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    var pendingCategoryId by remember(currentCategoryId) { mutableStateOf(currentCategoryId) }
    val hapticTick = rememberHapticTick()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.category_selector_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item(key = "no_category") {
                        CategoryOptionRow(
                            text = stringResource(id = R.string.category_no_category),
                            selected = pendingCategoryId == null,
                            onClick = {
                                hapticTick()
                                pendingCategoryId = null
                            }
                        )
                    }
                    if (customCategories.isNotEmpty()) {
                        item(key = "custom_header") {
                            CategorySectionHeader(text = stringResource(id = R.string.category_section_custom))
                        }
                        items(customCategories, key = { "custom_${it.id}" }) { category ->
                            CategoryOptionRow(
                                text = category.name,
                                selected = pendingCategoryId == category.id,
                                onClick = {
                                    hapticTick()
                                    pendingCategoryId = category.id
                                }
                            )
                        }
                    }
                    item(key = "default_header") {
                        CategorySectionHeader(text = stringResource(id = R.string.category_section_default))
                    }
                    items(DefaultCategory.values().toList(), key = { "default_${it.id}" }) { defaultCategory ->
                        CategoryOptionRow(
                            text = stringResource(id = defaultCategory.labelRes()),
                            selected = pendingCategoryId == defaultCategory.id,
                            onClick = {
                                hapticTick()
                                pendingCategoryId = defaultCategory.id
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        hapticTick()
                        onConfirm(pendingCategoryId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.category_selector_choose_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        hapticTick()
                        onAddCategoryClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.add_category_button),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

/** One selectable row inside [CategorySelectorDialog] - a highlighted (secondaryContainer) fill
 * plus a checkmark is this dialog's own selected-state indicator, distinct from every other
 * picker in the app (SelectionChip pills) since a scrollable list of rows, not a wrapping chip
 * row, is what fits a potentially-long category list. */
@Composable
private fun CategoryOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Box(modifier = Modifier.height(1.dp))
        }
    }
}
