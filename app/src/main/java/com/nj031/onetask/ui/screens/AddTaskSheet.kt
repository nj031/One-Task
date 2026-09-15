package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private enum class DateOption { TODAY, TOMORROW, CHOOSE_DATE }
private enum class RepeatOption { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddTaskSheetContent(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            onCancel = ::dismiss,
            onAddTask = { /* no-op: task creation not implemented yet */ }
        )
    }
}

@Composable
private fun AddTaskSheetContent(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onAddTask: () -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(DateOption.TODAY) }
    var selectedRepeat by remember { mutableStateOf(RepeatOption.NONE) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var postponeIfIncomplete by remember { mutableStateOf(false) }

    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.add_task_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        SectionLabel(text = stringResource(id = R.string.task_name_label))
        TextField(
            value = taskName,
            onValueChange = { taskName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        SectionLabel(text = stringResource(id = R.string.subtasks_label), topPadding = 20.dp)
        TonalActionButton(
            text = stringResource(id = R.string.add_subtask),
            onClick = { /* no-op: subtasks not implemented yet */ },
            modifier = Modifier.padding(top = 8.dp)
        )

        SectionLabel(text = stringResource(id = R.string.timer_label), topPadding = 20.dp)
        TonalActionButton(
            text = stringResource(id = R.string.add_timer),
            onClick = { /* no-op: timer not implemented yet */ },
            modifier = Modifier.padding(top = 8.dp)
        )

        SectionLabel(text = stringResource(id = R.string.date_label), topPadding = 20.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.today),
                selected = selectedDate == DateOption.TODAY,
                onClick = { selectedDate = DateOption.TODAY },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.date_tomorrow),
                selected = selectedDate == DateOption.TOMORROW,
                onClick = { selectedDate = DateOption.TOMORROW },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.date_choose),
                selected = selectedDate == DateOption.CHOOSE_DATE,
                onClick = { selectedDate = DateOption.CHOOSE_DATE },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(id = R.string.task_will_be_added_to, today),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        SectionLabel(text = stringResource(id = R.string.repeat_label), topPadding = 20.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.repeat_none),
                selected = selectedRepeat == RepeatOption.NONE,
                onClick = { selectedRepeat = RepeatOption.NONE },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.repeat_daily),
                selected = selectedRepeat == RepeatOption.DAILY,
                onClick = { selectedRepeat = RepeatOption.DAILY },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.repeat_weekly),
                selected = selectedRepeat == RepeatOption.WEEKLY,
                onClick = { selectedRepeat = RepeatOption.WEEKLY },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionChip(
                text = stringResource(id = R.string.repeat_monthly),
                selected = selectedRepeat == RepeatOption.MONTHLY,
                onClick = { selectedRepeat = RepeatOption.MONTHLY },
                modifier = Modifier.weight(1f)
            )
            SelectionChip(
                text = stringResource(id = R.string.repeat_custom),
                selected = selectedRepeat == RepeatOption.CUSTOM,
                onClick = { selectedRepeat = RepeatOption.CUSTOM },
                modifier = Modifier.weight(1f)
            )
        }

        SectionLabel(text = stringResource(id = R.string.tag_label), topPadding = 20.dp)
        TagsRow(
            selectedTags = selectedTags,
            onToggleTag = { tag ->
                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.postpone_if_incomplete),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = postponeIfIncomplete,
                onCheckedChange = { postponeIfIncomplete = it }
            )
        }

        TonalActionButton(
            text = stringResource(id = R.string.add_task_title),
            onClick = onAddTask,
            modifier = Modifier.padding(top = 24.dp),
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding)
    )
}

@Composable
private fun TonalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(text = text, fontWeight = fontWeight)
    }
}

@Composable
private fun RowScope.SelectionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tags = listOf(
        stringResource(id = R.string.tag_personal),
        stringResource(id = R.string.tag_work),
        stringResource(id = R.string.tag_study),
        stringResource(id = R.string.tag_health)
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val selected = tag in selectedTags
            FilterChip(
                selected = selected,
                onClick = { onToggleTag(tag) },
                label = {
                    Text(
                        text = tag,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(50),
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        AssistChip(
            onClick = { /* no-op: custom tags not implemented yet */ },
            label = { Text(text = stringResource(id = R.string.add_tag)) },
            shape = RoundedCornerShape(50),
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}
