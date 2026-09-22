package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.settings.NotesViewMode
import com.nj031.onetask.data.settings.TimeFormat
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.CompactBottomSheet
import com.nj031.onetask.ui.components.OneTaskBottomNav
import com.nj031.onetask.ui.components.ProfileAvatar
import com.nj031.onetask.ui.components.WallpaperBackdrop
import com.nj031.onetask.ui.theme.OneTaskArchiveIcon
import com.nj031.onetask.ui.theme.OneTaskCardViewIcon
import com.nj031.onetask.ui.theme.OneTaskLabelIcon
import com.nj031.onetask.ui.theme.OneTaskListViewIcon
import com.nj031.onetask.ui.theme.OneTaskRecycleBinIcon
import com.nj031.onetask.ui.theme.OneTaskSearchIcon
import com.nj031.onetask.ui.theme.OneTaskWallpapers
import com.nj031.onetask.viewmodel.JournalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    viewModel: JournalViewModel = viewModel(),
    profilePhotoPath: String? = null,
    onAddNoteClick: (JournalNoteType) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onOpenTimerPlaceholder: () -> Unit = {},
    onProfileAvatarClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onRecycleBinClick: () -> Unit = {},
    onLabelsClick: () -> Unit = {},
    timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    wallpaper: Wallpaper = Wallpaper.NONE,
    darkTheme: Boolean = false
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var actionMenuNote by remember { mutableStateOf<JournalNoteEntity?>(null) }

    BackHandler(enabled = actionMenuNote != null) {
        actionMenuNote = null
    }

    // Notes is one of only 3 screens the wallpaper IMAGE itself is scoped to (see the Wallpaper
    // spec's "image scope" rule) - WallpaperBackdrop is a no-op when no wallpaper is selected, so
    // this Box changes nothing about this screen's existing look/behavior in that case.
    Box(modifier = Modifier.fillMaxSize()) {
    WallpaperBackdrop(wallpaper = wallpaper, darkTheme = darkTheme)
    Scaffold(
        containerColor = if (wallpaper == Wallpaper.NONE) MaterialTheme.colorScheme.background else Color.Transparent,
        bottomBar = {
            OneTaskBottomNav(
                activeTab = BottomNavTab.JOURNAL,
                onJournalClick = {},
                onTasksClick = onNavigateToTasks,
                onTimerClick = onOpenTimerPlaceholder,
                backgroundColor = OneTaskWallpapers.definitionFor(wallpaper)?.let {
                    if (darkTheme) it.dark.bottomNavigation else it.light.bottomNavigation
                } ?: MaterialTheme.colorScheme.surface,
                elevated = wallpaper != Wallpaper.NONE
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
                NotesTopBar(
                    profilePhotoPath = profilePhotoPath,
                    onAvatarClick = onProfileAvatarClick,
                    onArchiveClick = onArchiveClick,
                    onRecycleBinClick = onRecycleBinClick,
                    onLabelsClick = onLabelsClick,
                    wallpaper = wallpaper
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotesSearchField(
                        query = searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                        modifier = Modifier.weight(1f)
                    )

                    NotesViewToggle(
                        viewMode = viewMode,
                        onViewModeChange = viewModel::setViewMode,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Button(
                    onClick = { onAddNoteClick(JournalNoteType.TEXT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    // Only while a wallpaper is active: gives this CTA a defined edge against
                    // whatever wallpaper pixels happen to sit behind it - the same border token/
                    // technique Cards elsewhere already use - without changing containerColor's
                    // own existing opacity. Non-wallpaper themes are unaffected.
                    border = if (wallpaper != Wallpaper.NONE) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    } else {
                        null
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(id = R.string.notes_add_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (notes.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.notes_list_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.no_notes_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                } else {
                    // notes is already sorted most-recently-edited first (see
                    // JournalViewModel.notes); partitioning it (rather than re-sorting) keeps
                    // that same relative order within each section, so "latest modified" ordering
                    // applies identically to the Pinned Notes section and the rest.
                    val pinnedNotes = notes.filter { it.pinned }
                    val unpinnedNotes = notes.filterNot { it.pinned }
                    when (viewMode) {
                        NotesViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(key = "pinned_heading") {
                                        NotesSectionHeading(
                                            text = stringResource(id = R.string.notes_pinned_heading),
                                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                                        )
                                    }
                                    items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                                        NoteListRow(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onLongClick = { actionMenuNote = note },
                                            wallpaper = wallpaper
                                        )
                                    }
                                }
                                if (unpinnedNotes.isNotEmpty()) {
                                    item(key = "notes_heading") {
                                        NotesSectionHeading(
                                            text = stringResource(id = R.string.notes_list_heading),
                                            modifier = Modifier.padding(
                                                top = if (pinnedNotes.isNotEmpty()) 20.dp else 24.dp,
                                                bottom = 12.dp
                                            )
                                        )
                                    }
                                    items(unpinnedNotes, key = { it.id }) { note ->
                                        NoteListRow(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onLongClick = { actionMenuNote = note },
                                            wallpaper = wallpaper
                                        )
                                    }
                                }
                            }
                        }
                        NotesViewMode.CARD -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(key = "pinned_heading", span = { GridItemSpan(maxLineSpan) }) {
                                        NotesSectionHeading(
                                            text = stringResource(id = R.string.notes_pinned_heading),
                                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                                        )
                                    }
                                    gridItems(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onLongClick = { actionMenuNote = note },
                                            wallpaper = wallpaper
                                        )
                                    }
                                }
                                if (unpinnedNotes.isNotEmpty()) {
                                    item(key = "notes_heading", span = { GridItemSpan(maxLineSpan) }) {
                                        NotesSectionHeading(
                                            text = stringResource(id = R.string.notes_list_heading),
                                            modifier = Modifier.padding(
                                                top = if (pinnedNotes.isNotEmpty()) 20.dp else 24.dp,
                                                bottom = 12.dp
                                            )
                                        )
                                    }
                                    gridItems(unpinnedNotes, key = { it.id }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onLongClick = { actionMenuNote = note },
                                            wallpaper = wallpaper
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    actionMenuNote?.let { note ->
        NoteActionSheet(
            isPinned = note.pinned,
            onPinToggleClick = {
                viewModel.setPinned(note, !note.pinned)
                actionMenuNote = null
            },
            onArchiveClick = {
                viewModel.archiveNote(note)
                actionMenuNote = null
            },
            onDeleteClick = {
                // No confirmation - deleted notes go straight to the Recycle Bin, where they can
                // still be restored or permanently removed with its own confirmation.
                viewModel.trashNote(note)
                actionMenuNote = null
            },
            onDismiss = { actionMenuNote = null }
        )
    }
}

@Composable
private fun NotesTopBar(
    profilePhotoPath: String?,
    onAvatarClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onLabelsClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val profileDescription = stringResource(id = R.string.nav_profile)
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Only while a wallpaper is active: gives this avatar/title/menu row the same
            // translucent-surface-plus-border treatment as this screen's own search field, since
            // (unlike that field) this header previously rendered directly over the wallpaper
            // image with nothing behind it - reusing the existing Verdant border/surface tokens,
            // not a new color. Non-wallpaper themes are unaffected.
            .then(
                if (wallpaper != Wallpaper.NONE) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                } else {
                    Modifier
                }
            )
    ) {
        IconButton(
            onClick = onAvatarClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .semantics { contentDescription = profileDescription }
        ) {
            ProfileAvatar(photoPath = profilePhotoPath, size = 32.dp)
        }

        Text(
            text = stringResource(id = R.string.journal_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.notes_more_options),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                // Same translucent-surface-plus-border treatment as the Notes search field (see
                // NotesSearchField) and Timer's own pills - only while a wallpaper is actually
                // active, so non-wallpaper themes are unaffected.
                modifier = if (wallpaper != Wallpaper.NONE) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.archive_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = { OneTaskArchiveIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    onClick = {
                        showMenu = false
                        onArchiveClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.recycle_bin_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = { OneTaskRecycleBinIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    onClick = {
                        showMenu = false
                        onRecycleBinClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.labels_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    leadingIcon = { OneTaskLabelIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp) },
                    onClick = {
                        showMenu = false
                        onLabelsClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun NotesViewToggle(
    viewMode: NotesViewMode,
    onViewModeChange: (NotesViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        NotesViewToggleButton(
            selected = viewMode == NotesViewMode.LIST,
            contentDescription = stringResource(id = R.string.notes_view_list),
            onClick = { onViewModeChange(NotesViewMode.LIST) }
        ) { tint -> OneTaskListViewIcon(tint = tint, size = 20.dp) }

        Spacer(modifier = Modifier.width(8.dp))

        NotesViewToggleButton(
            selected = viewMode == NotesViewMode.CARD,
            contentDescription = stringResource(id = R.string.notes_view_card),
            onClick = { onViewModeChange(NotesViewMode.CARD) }
        ) { tint -> OneTaskCardViewIcon(tint = tint, size = 20.dp) }
    }
}

@Composable
private fun NotesViewToggleButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        icon(tint)
    }
}

@Composable
private fun NotesSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        placeholder = {
            Text(
                text = stringResource(id = R.string.notes_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = { OneTaskSearchIcon(size = 20.dp) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteListRow(
    note: JournalNoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        // Same translucent Card surface Timer's own pills/segmented control already pair with a
        // MaterialTheme.colorScheme.outline border for definition against the wallpaper backdrop
        // - only while a wallpaper is actually active, so non-wallpaper themes are unaffected.
        border = if (wallpaper != Wallpaper.NONE) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // The title/label header row is only present when there's something to show in it -
            // an untitled, unlabeled note keeps the old layout (preview text starts right at the
            // top of the card) exactly as it was.
            if (note.title.isNotBlank() || note.label != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    note.label?.let { label ->
                        NoteLabelPill(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.previewText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = note.updatedAt.toDisplayDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: JournalNoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        // Same translucent Card surface Timer's own pills/segmented control already pair with a
        // MaterialTheme.colorScheme.outline border for definition against the wallpaper backdrop
        // - only while a wallpaper is actually active, so non-wallpaper themes are unaffected.
        border = if (wallpaper != Wallpaper.NONE) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .height(120.dp)
        ) {
            if (note.title.isNotBlank() || note.label != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    note.label?.let { label ->
                        NoteLabelPill(text = label, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = note.previewText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = note.updatedAt.toDisplayDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Shows a note's assigned Label (if any) at the top-right of its card, in both list and grid
 * view - same small outlined-pill treatment Task cards already use for a Task's Category (see
 * HomeScreen's own TagPill), not a new visual role. Nothing is rendered for a note with no Label
 * assigned (see both call sites' own note.label?.let). */
@Composable
private fun NoteLabelPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteActionSheet(
    isPinned: Boolean,
    onPinToggleClick: () -> Unit,
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
            NoteActionSheetItem(
                text = stringResource(id = if (isPinned) R.string.note_menu_unpin_note else R.string.note_menu_pin_note),
                onClick = { dismissThen(onPinToggleClick) }
            )
            NoteActionSheetItem(
                text = stringResource(id = R.string.archive),
                onClick = { dismissThen(onArchiveClick) }
            )
            NoteActionSheetItem(
                text = stringResource(id = R.string.delete),
                onClick = { dismissThen(onDeleteClick) }
            )
            NoteActionSheetItem(
                text = stringResource(id = R.string.cancel),
                onClick = { dismissThen(onDismiss) }
            )
        }
    }
}

@Composable
private fun NoteActionSheetItem(text: String, onClick: () -> Unit) {
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

/** One "Pinned Notes"/"Notes" section heading inside the notes list - same style the list's own
 * single heading already used, now reusable since there can be up to two of them. */
@Composable
private fun NotesSectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

private const val NOTE_PREVIEW_MAX_LENGTH = 60

private fun JournalNoteEntity.previewText(): String = when (noteType) {
    JournalNoteType.TEXT -> content.toPreviewText()
    JournalNoteType.CHECKLIST -> checklistItems.joinToString(separator = "   ") { it.text }.toPreviewText()
}

/** The note's last-updated date, not time-of-day - matches the "MMM d, yyyy" date format already
 * used elsewhere in the app (e.g. the Tasks homepage's selected-date label). */
private fun Long.toDisplayDate(): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatter)
}

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
