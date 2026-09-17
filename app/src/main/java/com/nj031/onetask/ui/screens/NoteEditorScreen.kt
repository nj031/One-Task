package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.viewmodel.JournalViewModel

/**
 * The single editor for both Text and Checklist notes - the note's [JournalNoteType] is fixed
 * at creation (via [noteType] for a brand-new note, or the existing note's own saved type when
 * reopened) and never changes; only the content area below the title differs by type.
 *
 * State here deliberately uses plain `remember`, not `rememberSaveable`: the app going to the
 * background or the screen locking never destroys this composition (Activity.onStop, not
 * onDestroy), so `remember` alone already preserves in-progress edits across both - exactly the
 * "preserve on background/lock, but not across a real process kill" behavior this needs, with no
 * extra plumbing.
 */
@Composable
fun NoteEditorScreen(
    viewModel: JournalViewModel = viewModel(),
    noteId: String? = null,
    noteType: JournalNoteType = JournalNoteType.TEXT,
    onDone: () -> Unit
) {
    val existingNote = remember(noteId) { viewModel.getNoteById(noteId) }
    val effectiveNoteType = existingNote?.noteType ?: noteType

    var title by remember { mutableStateOf(existingNote?.title.orEmpty()) }
    var contentValue by remember { mutableStateOf(TextFieldValue(existingNote?.content.orEmpty())) }
    var checklistItems by remember {
        mutableStateOf(
            (existingNote?.checklistItems ?: emptyList()).ifEmpty {
                if (effectiveNoteType == JournalNoteType.CHECKLIST) listOf(ChecklistItem(text = "")) else emptyList()
            }
        )
    }

    // A note only "has content" worth keeping when the title or the type-specific content is
    // non-blank - an empty checklist item created just by opening Add > Checklist doesn't count
    // on its own, matching how an untouched Text note (title blank, content blank) never saves.
    fun hasContent(): Boolean = when (effectiveNoteType) {
        JournalNoteType.TEXT -> title.isNotBlank() || contentValue.text.isNotBlank()
        JournalNoteType.CHECKLIST -> title.isNotBlank() || checklistItems.any { it.text.isNotBlank() }
    }

    fun commitOnExit() {
        val note = existingNote
        if (hasContent()) {
            val savedItems = checklistItems.filter { it.text.isNotBlank() }
            if (note != null) {
                viewModel.updateNote(note = note, title = title, content = contentValue.text, checklistItems = savedItems)
            } else {
                viewModel.createNote(
                    title = title,
                    content = contentValue.text,
                    noteType = effectiveNoteType,
                    checklistItems = savedItems
                )
            }
        } else if (note != null) {
            // Every field was cleared out on an existing note - it shouldn't linger as an empty
            // note, so it's discarded outright rather than saved with nothing in it.
            viewModel.deleteEmptyNote(note)
        }
        // A brand-new note that was never given any content simply was never created - nothing
        // to undo.
    }

    BackHandler {
        commitOnExit()
        onDone()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Scaffold's own content insets don't include the IME by default (so text
                // fields aren't force-pushed up on every screen, even ones with no input).
                // Consuming it here shrinks this Box's visible height as the keyboard
                // animates in, which - combined with the Column's own verticalScroll below -
                // is what lets it actually scroll far enough to reach lower content, and lets
                // each TextField's built-in cursor-follow behavior bring the current line back
                // above the keyboard as the user types, without any extra scroll plumbing.
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentScrollState = rememberScrollState()

            // The content TextField's built-in cursor-follow behavior handles keeping the
            // active line visible on every keystroke except the very first time the note
            // grows past the visible viewport, where its internal bring-into-view calculation
            // can run before imePadding()'s own animation has settled. This is a deterministic
            // backstop: whenever the cursor is collapsed at the exact end of the text (i.e. the
            // user is actively typing forward, not editing mid-note), force-scroll to the true
            // bottom. It never fires during mid-note edits or manual scrolling elsewhere.
            LaunchedEffect(contentValue) {
                if (contentValue.selection.collapsed && contentValue.selection.end == contentValue.text.length) {
                    contentScrollState.animateScrollTo(contentScrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                NoteEditorTopBar(onBackClick = { commitOnExit(); onDone() })

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.note_title_placeholder),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true,
                    colors = transparentTextFieldColors()
                )

                when (effectiveNoteType) {
                    JournalNoteType.TEXT -> {
                        TextField(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.note_content_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            colors = transparentTextFieldColors()
                        )
                    }
                    JournalNoteType.CHECKLIST -> {
                        ChecklistEditor(
                            items = checklistItems,
                            onItemsChange = { checklistItems = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorTopBar(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.back),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** The checklist content area: an ordered (not reorderable) list of items, each a circular
 * checkbox plus its text, and a trailing "Add item" row. Newly added items are focused
 * automatically so the user can start typing right away. */
@Composable
private fun ChecklistEditor(items: List<ChecklistItem>, onItemsChange: (List<ChecklistItem>) -> Unit) {
    var focusTargetId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        items.forEach { item ->
            ChecklistItemRow(
                item = item,
                requestFocus = focusTargetId == item.id,
                onFocusHandled = { focusTargetId = null },
                onCheckedChange = { checked ->
                    onItemsChange(items.map { if (it.id == item.id) it.copy(checked = checked) else it })
                },
                onTextChange = { text ->
                    onItemsChange(items.map { if (it.id == item.id) it.copy(text = text) else it })
                },
                onDeleteClick = {
                    onItemsChange(items.filterNot { it.id == item.id })
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newItem = ChecklistItem(text = "")
                    onItemsChange(items + newItem)
                    focusTargetId = newItem.id
                }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(id = R.string.add_checklist_item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val focusRequester = remember(item.id) { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChecklistCheckbox(checked = item.checked, onToggle = { onCheckedChange(!item.checked) })

        TextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = stringResource(id = R.string.checklist_item_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            // Deliberately no strike-through/decoration on the text regardless of [checked] -
            // completed checklist items look exactly like unchecked ones apart from the box.
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine = true,
            colors = transparentTextFieldColors()
        )

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A simple circular checkbox (tick/untick only, no strike-through) - Material3 only ships a
 * rounded-square Checkbox, so this is drawn directly to match the compact circular style. */
@Composable
private fun ChecklistCheckbox(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val checkPath = Path().apply {
                    moveTo(size.width * 0.06f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.92f)
                    lineTo(size.width * 0.96f, size.height * 0.12f)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(width = size.minDimension * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent
)
