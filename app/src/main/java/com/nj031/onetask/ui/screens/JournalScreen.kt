package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.viewmodel.JournalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = viewModel(),
    onAddNoteClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val notes by viewModel.notesForSelectedDate.collectAsState()
    val activeNoteDates by viewModel.activeNoteDates.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var actionMenuNote by remember { mutableStateOf<JournalNoteEntity?>(null) }
    var pendingDeleteNote by remember { mutableStateOf<JournalNoteEntity?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // System back priority: drawer > long-press action menu > delete confirmation dialog
    // > (default) return to the previous screen. Only one of these is ever open at a
    // time in practice, so registration order among them doesn't matter.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = actionMenuNote != null) {
        actionMenuNote = null
    }
    BackHandler(enabled = pendingDeleteNote != null) {
        pendingDeleteNote = null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.6f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                OneTaskDrawerContent(
                    userEmail = AuthRepository.currentUser?.email
                        ?: stringResource(id = R.string.sample_user_email),
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    onHelpFeedbackClick = { /* no-op: help & feedback not implemented yet */ },
                    onRateAppClick = { /* no-op: not published yet */ },
                    onArchiveClick = {
                        scope.launch { drawerState.close() }
                        onArchiveClick()
                    },
                    onRecycleBinClick = {
                        scope.launch { drawerState.close() }
                        onRecycleBinClick()
                    },
                    onLogoutClick = onLogout
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddNoteClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add_note)
                    )
                }
            },
            bottomBar = {
                OneTaskBottomNav(
                    activeTab = BottomNavTab.JOURNAL,
                    onJournalClick = {},
                    onTasksClick = onNavigateToTasks,
                    onProfileClick = onNavigateToProfile
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
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    JournalTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCalendarClick = { showDatePicker = true }
                    )

                    Text(
                        text = selectedDate.format(journalDateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )

                    if (notes.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.no_notes_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                JournalNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onLongClick = { actionMenuNote = note }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        OneTaskCalendarSheet(
            initialDate = selectedDate,
            markedDates = activeNoteDates,
            maxSelectableDate = LocalDate.now(),
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    actionMenuNote?.let { note ->
        JournalNoteActionSheet(
            onArchiveClick = {
                viewModel.archiveNote(note)
                actionMenuNote = null
            },
            onDeleteClick = {
                actionMenuNote = null
                pendingDeleteNote = note
            },
            onDismiss = { actionMenuNote = null }
        )
    }

    pendingDeleteNote?.let { note ->
        DeleteNoteConfirmationDialog(
            onConfirm = {
                viewModel.trashNote(note)
                pendingDeleteNote = null
            },
            onDismiss = { pendingDeleteNote = null }
        )
    }
}

@Composable
private fun JournalTopBar(onMenuClick: () -> Unit, onCalendarClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(id = R.string.menu),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = stringResource(id = R.string.journal_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = stringResource(id = R.string.calendar),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalNoteCard(
    note: JournalNoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.content.toPreviewText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = note.updatedAt.toDisplayTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalNoteActionSheet(
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            JournalActionSheetItem(
                text = stringResource(id = R.string.archive),
                onClick = { dismissThen(onArchiveClick) }
            )
            JournalActionSheetItem(
                text = stringResource(id = R.string.delete),
                onClick = { dismissThen(onDeleteClick) }
            )
            JournalActionSheetItem(
                text = stringResource(id = R.string.cancel),
                onClick = { dismissThen(onDismiss) }
            )
        }
    }
}

@Composable
private fun JournalActionSheetItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteNoteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.delete_note_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.delete_note_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            FilledTonalButton(
                onClick = { dismissThen(onConfirm) },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(id = R.string.delete), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(
                onClick = { dismissThen(onDismiss) },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val journalDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

private val noteTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private const val NOTE_PREVIEW_MAX_LENGTH = 60

private fun Long.toDisplayTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(noteTimeFormatter)

internal fun String.toPreviewText(): String {
    val collapsed = lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    return if (collapsed.length > NOTE_PREVIEW_MAX_LENGTH) {
        collapsed.take(NOTE_PREVIEW_MAX_LENGTH).trimEnd() + "…"
    } else {
        collapsed
    }
}
