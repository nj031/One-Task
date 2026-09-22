package com.nj031.onetask.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.journal.NoteFormatSpan
import com.nj031.onetask.data.journal.NoteFormatStyle
import com.nj031.onetask.viewmodel.JournalViewModel
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * The single editor for both Text and Checklist notes - the note's [JournalNoteType] is fixed
 * at creation (via [noteType] for a brand-new note, or the existing note's own saved type when
 * reopened) and never changes; only the content area below the title differs by type.
 *
 * State here deliberately uses plain `remember`, not `rememberSaveable`: the app going to the
 * background or the screen locking never destroys this composition (Activity.onStop, not
 * onDestroy), so `remember` alone already preserves in-progress edits across both - exactly the
 * "preserve on background/lock, but not across a real process kill" behavior this needs, with no
 * extra plumbing. The same is true of navigating to Labels and back (see [onManageLabelsClick]
 * below) - it's a normal back-stack push, not a recreation, so this composition (and every var
 * below) survives the round trip unchanged.
 *
 * Bold/Italic/Underline/Aa formatting ([formatSpans]) only applies to a TEXT note's [contentValue]
 * - see [NoteFormatSpan]'s own doc comment. It is deferred-saved exactly like [title]/[contentValue]
 * themselves, through the same [commitOnExit] path, so it follows this app's one existing Note
 * persistence mechanism (Room + the per-note Firestore push) rather than a separate one.
 */
@Composable
fun NoteEditorScreen(
    viewModel: JournalViewModel = viewModel(),
    noteId: String? = null,
    noteType: JournalNoteType = JournalNoteType.TEXT,
    onDone: () -> Unit,
    onManageLabelsClick: () -> Unit = {}
) {
    val existingNote = remember(noteId) { viewModel.getNoteById(noteId) }
    val effectiveNoteType = existingNote?.noteType ?: noteType
    // The id a brand-new note will be created under - generated once, up front, so the eager
    // create below (see the hasContentNow LaunchedEffect) and commitOnExit's own fallback create
    // always target the exact same row via JournalNoteDao.insert's REPLACE conflict strategy,
    // even if both somehow ever fired (they can't produce two notes for one editing session).
    val pendingNoteId = remember(noteId) { noteId ?: UUID.randomUUID().toString() }
    // The note's real, currently-persisted DB row - null for a brand-new note until it first gets
    // real content (see the hasContentNow LaunchedEffect below). Distinct from [existingNote],
    // which is captured once at composition entry and never updates: Pin/Archive/Delete and Note
    // Info must react the moment a new note becomes real, without needing the editor to be closed
    // and reopened (which is what used to create a fresh, non-null existingNote instead).
    var persistedNote by remember(noteId) { mutableStateOf(existingNote) }

    var title by remember { mutableStateOf(existingNote?.title.orEmpty()) }
    var contentValue by remember { mutableStateOf(TextFieldValue(existingNote?.content.orEmpty())) }
    var checklistItems by remember {
        mutableStateOf(
            (existingNote?.checklistItems ?: emptyList()).ifEmpty {
                if (effectiveNoteType == JournalNoteType.CHECKLIST) listOf(ChecklistItem(text = "")) else emptyList()
            }
        )
    }
    var noteLabel by remember { mutableStateOf(existingNote?.label) }
    var isPinned by remember { mutableStateOf(existingNote?.pinned ?: false) }
    var formatSpans by remember { mutableStateOf(existingNote?.contentFormatSpans ?: emptyList()) }
    // Armed while the selection is collapsed: the next characters typed inherit whichever of
    // these styles are armed (see the content TextField's onValueChange below) - the "start
    // applying from the current cursor/typing position" behavior Bold/Italic/Underline need when
    // nothing is selected, mirroring an ordinary word processor's own "type in Bold" behavior.
    var pendingCharacterStyles by remember { mutableStateOf(emptySet<NoteFormatStyle>()) }

    var showLabelDialog by remember { mutableStateOf(false) }
    var showNoteInfoDialog by remember { mutableStateOf(false) }
    val labels by viewModel.labels.collectAsState()
    val context = LocalContext.current

    // A note only "has content" worth keeping when the title or the type-specific content is
    // non-blank - an empty checklist item created just by opening Add > Checklist doesn't count
    // on its own, matching how an untouched Text note (title blank, content blank) never saves.
    fun hasContent(): Boolean = when (effectiveNoteType) {
        JournalNoteType.TEXT -> title.isNotBlank() || contentValue.text.isNotBlank()
        JournalNoteType.CHECKLIST -> title.isNotBlank() || checklistItems.any { it.text.isNotBlank() }
    }

    fun commitOnExit() {
        val note = persistedNote
        if (hasContent()) {
            val savedItems = checklistItems.filter { it.text.isNotBlank() }
            if (note != null) {
                viewModel.updateNote(
                    note = note,
                    title = title,
                    content = contentValue.text,
                    checklistItems = savedItems,
                    label = noteLabel,
                    contentFormatSpans = formatSpans,
                    // Re-asserts whatever togglePin() already wrote immediately (see its own
                    // comment) rather than reverting it - note (the composition-time snapshot)
                    // never itself observes a pin toggle that happened after this screen opened.
                    pinned = isPinned
                )
            } else {
                // Fallback only - the hasContentNow LaunchedEffect below already eagerly creates
                // the note the moment it gets real content, so persistedNote is normally non-null
                // well before the user can reach this exit path. This exists purely to cover a
                // narrow race (exiting in the same instant content first appears, before that
                // effect's own create call has completed) - it targets the exact same
                // pendingNoteId, so JournalNoteDao.insert's REPLACE conflict strategy collapses
                // the two into one row rather than creating a duplicate note either way.
                viewModel.createNote(
                    title = title,
                    content = contentValue.text,
                    noteType = effectiveNoteType,
                    checklistItems = savedItems,
                    label = noteLabel,
                    contentFormatSpans = formatSpans,
                    pinned = isPinned,
                    id = pendingNoteId
                )
            }
        } else if (note != null) {
            // Every field was cleared back out - including on a note the hasContentNow
            // LaunchedEffect already eagerly created earlier in this same session - so it
            // shouldn't linger as an empty note; it's discarded outright rather than saved with
            // nothing in it.
            viewModel.deleteEmptyNote(note)
        }
        // A brand-new note that was never given any content simply was never created - nothing
        // to undo.
    }

    fun performArchive() {
        persistedNote?.let { viewModel.archiveNote(it) }
        onDone()
    }

    fun performDelete() {
        persistedNote?.let { viewModel.trashNote(it) }
        onDone()
    }

    // Immediate (not deferred-through-commitOnExit) write - see
    // JournalRepository.setPinned's own doc comment for why Pin/Unpin needs this rather than the
    // label/formatting fields' deferred-save pattern. isPinned is also threaded into
    // commitOnExit's own updateNote(...) call above so a later deferred save can't revert this.
    fun togglePin() {
        val note = persistedNote ?: return
        isPinned = !isPinned
        viewModel.setPinned(note, isPinned)
    }

    fun performShare() {
        val body = when (effectiveNoteType) {
            JournalNoteType.TEXT -> contentValue.text
            JournalNoteType.CHECKLIST -> checklistItems
                .filter { it.text.isNotBlank() }
                .joinToString(separator = "\n") { "${if (it.checked) "☑" else "☐"} ${it.text}" }
        }
        val shareText = if (title.isNotBlank()) "$title\n\n$body" else body
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    val selection = contentValue.selection
    fun isCharacterStyleActive(style: NoteFormatStyle): Boolean =
        if (selection.collapsed) style in pendingCharacterStyles else isRangeFullyCovered(formatSpans, style, selection.min, selection.max)

    fun toggleCharacterStyle(style: NoteFormatStyle) {
        if (!selection.collapsed) {
            formatSpans = toggleStyleOverRange(formatSpans, style, selection.min, selection.max)
        } else {
            pendingCharacterStyles = if (style in pendingCharacterStyles) {
                pendingCharacterStyles - style
            } else {
                pendingCharacterStyles + style
            }
        }
    }

    fun applyHeading(style: NoteFormatStyle?) {
        if (!selection.collapsed) {
            formatSpans = setHeadingOverRange(formatSpans, style, selection.min, selection.max)
        } else {
            // No selection: mirrors Bold/Italic/Underline's own pendingCharacterStyles behavior
            // above - arms the chosen size for the NEXT characters typed at the cursor rather
            // than retroactively resizing whatever's already on the current line (previously this
            // branch, when the cursor sat on an empty line - which is exactly the Note Editor's
            // own starting state on a new note - always formatted a zero-length range and so
            // never had any visible effect, and even when it hit non-empty text, newly typed text
            // right after it silently reverted to the normal body size instead of continuing it).
            // HEADING_MEDIUM/HEADING_LARGE are mutually exclusive (see setHeadingOverRange's own
            // comment), so choosing one always clears the other from the pending set first.
            pendingCharacterStyles = pendingCharacterStyles - NoteFormatStyle.HEADING_MEDIUM - NoteFormatStyle.HEADING_LARGE
            if (style != null) {
                pendingCharacterStyles = pendingCharacterStyles + style
            }
        }
    }

    // Eagerly persists a brand-new note the moment it first gets real content, instead of only
    // on exit (commitOnExit's own create path) - Pin/Archive/Delete and Note Info all need a real
    // DB row (persistedNote) to act on, and previously had none until the editor was closed and
    // reopened. Keyed on the boolean itself (not on title/contentValue/checklistItems), so it
    // fires exactly once per editing session, on the false->true edge - keying on the raw content
    // would restart (and so never let complete) this coroutine on every keystroke typed before
    // the previous attempt's local Room insert finished.
    val hasContentNow = hasContent()
    LaunchedEffect(hasContentNow) {
        if (hasContentNow && persistedNote == null) {
            persistedNote = viewModel.createNoteAwait(
                title = title,
                content = contentValue.text,
                noteType = effectiveNoteType,
                checklistItems = checklistItems.filter { it.text.isNotBlank() },
                label = noteLabel,
                contentFormatSpans = formatSpans,
                pinned = isPinned,
                id = pendingNoteId
            )
        }
    }

    BackHandler {
        commitOnExit()
        onDone()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NoteFormattingToolbar(
                formattingEnabled = effectiveNoteType == JournalNoteType.TEXT,
                isBoldActive = isCharacterStyleActive(NoteFormatStyle.BOLD),
                isItalicActive = isCharacterStyleActive(NoteFormatStyle.ITALIC),
                isUnderlineActive = isCharacterStyleActive(NoteFormatStyle.UNDERLINE),
                onBoldClick = { toggleCharacterStyle(NoteFormatStyle.BOLD) },
                onItalicClick = { toggleCharacterStyle(NoteFormatStyle.ITALIC) },
                onUnderlineClick = { toggleCharacterStyle(NoteFormatStyle.UNDERLINE) },
                onSizeSelected = { style -> applyHeading(style) },
                // Bulleted list / Checklist toolbar buttons and both + menu options are
                // placeholders only, per this task's own spec - deliberately no-op.
                onBulletedListClick = {},
                onChecklistClick = {},
                onAddImageClick = {},
                onOpenCameraClick = {}
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding.bottom already reflects the keyboard: NoteFormattingToolbar (this
                // Scaffold's bottomBar) applies windowInsetsPadding(navigationBars.union(ime)) to
                // itself, so its own measured height - which Scaffold uses to compute
                // innerPadding - already grows by the IME's height while the keyboard is open. A
                // separate .imePadding() here used to subtract that same IME height a second
                // time, collapsing this Box's available height to only a couple of lines and
                // leaving a large blank gap between the shrunk content and the (correctly
                // positioned) toolbar/keyboard below it. Consuming innerPadding alone is enough -
                // the IME inset only needs to be accounted for once in this chain.
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (effectiveNoteType) {
                JournalNoteType.TEXT -> {
                    val contentScrollState = rememberScrollState()

                    // The content TextField's built-in cursor-follow behavior handles keeping the
                    // active line visible on every keystroke except the very first time the note
                    // grows past the visible viewport, where its internal bring-into-view
                    // calculation can run before imePadding()'s own animation has settled. This is
                    // a deterministic backstop: whenever the cursor is collapsed at the exact end
                    // of the text (i.e. the user is actively typing forward, not editing
                    // mid-note), force-scroll to the true bottom. It never fires during mid-note
                    // edits or manual scrolling elsewhere.
                    LaunchedEffect(contentValue) {
                        if (contentValue.selection.collapsed && contentValue.selection.end == contentValue.text.length) {
                            contentScrollState.animateScrollTo(contentScrollState.maxValue)
                        }
                    }

                    val formatTransformation = remember(formatSpans) { noteFormatVisualTransformation(formatSpans) }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .verticalScroll(contentScrollState)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        NoteEditorTopBar(
                            onBackClick = { commitOnExit(); onDone() },
                            canModifyNote = persistedNote != null,
                            isPinned = isPinned,
                            onShareClick = ::performShare,
                            onAddToLabelClick = { showLabelDialog = true },
                            onPinToggleClick = ::togglePin,
                            onArchiveClick = ::performArchive,
                            onDeleteClick = ::performDelete,
                            onNoteInfoClick = { showNoteInfoDialog = true }
                        )

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
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = transparentTextFieldColors()
                        )

                        TextField(
                            value = contentValue,
                            onValueChange = { newValue ->
                                val diff = diffText(contentValue.text, newValue.text)
                                var updatedSpans = shiftSpansForEdit(formatSpans, diff)
                                if (pendingCharacterStyles.isNotEmpty() && diff.newEnd > diff.oldStart) {
                                    pendingCharacterStyles.forEach { style ->
                                        updatedSpans = addCoverage(updatedSpans, style, diff.oldStart, diff.newEnd)
                                    }
                                }
                                formatSpans = updatedSpans
                                contentValue = newValue
                            },
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
                            visualTransformation = formatTransformation,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = transparentTextFieldColors()
                        )
                    }
                }
                JournalNoteType.CHECKLIST -> {
                    // Unlike TEXT mode, the checklist owns its own scrolling (a LazyColumn, so it
                    // stays smooth and light with 50+ items) - the back bar and title sit above it,
                    // fixed, rather than sharing one big verticalScroll container with the items.
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        NoteEditorTopBar(
                            onBackClick = { commitOnExit(); onDone() },
                            canModifyNote = persistedNote != null,
                            isPinned = isPinned,
                            onShareClick = ::performShare,
                            onAddToLabelClick = { showLabelDialog = true },
                            onPinToggleClick = ::togglePin,
                            onArchiveClick = ::performArchive,
                            onDeleteClick = ::performDelete,
                            onNoteInfoClick = { showNoteInfoDialog = true }
                        )

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
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = transparentTextFieldColors()
                        )

                        ChecklistEditor(
                            items = checklistItems,
                            onItemsChange = { checklistItems = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLabelDialog) {
        NoteLabelSelectorDialog(
            currentLabel = noteLabel,
            labels = labels,
            onDismiss = { showLabelDialog = false },
            onConfirm = { chosen ->
                noteLabel = chosen
                showLabelDialog = false
            },
            onAddLabelClick = {
                showLabelDialog = false
                onManageLabelsClick()
            }
        )
    }

    val noteInfoTarget = persistedNote
    if (showNoteInfoDialog && noteInfoTarget != null) {
        NoteInfoDialog(
            note = noteInfoTarget,
            title = title,
            noteLabel = noteLabel,
            onDismiss = { showNoteInfoDialog = false }
        )
    }
}

@Composable
private fun NoteEditorTopBar(
    onBackClick: () -> Unit,
    canModifyNote: Boolean,
    isPinned: Boolean,
    onShareClick: () -> Unit,
    onAddToLabelClick: () -> Unit,
    onPinToggleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNoteInfoClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val disabledColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back),
                tint = MaterialTheme.colorScheme.primary
            )
        }

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
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.note_menu_share)) },
                    onClick = { showMenu = false; onShareClick() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.note_menu_add_to_label)) },
                    onClick = { showMenu = false; onAddToLabelClick() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                id = if (isPinned) R.string.note_menu_unpin_note else R.string.note_menu_pin_note
                            ),
                            color = if (canModifyNote) MaterialTheme.colorScheme.onBackground else disabledColor
                        )
                    },
                    enabled = canModifyNote,
                    onClick = { showMenu = false; onPinToggleClick() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.archive),
                            color = if (canModifyNote) MaterialTheme.colorScheme.onBackground else disabledColor
                        )
                    },
                    enabled = canModifyNote,
                    onClick = { showMenu = false; onArchiveClick() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.delete),
                            color = if (canModifyNote) MaterialTheme.colorScheme.error else disabledColor
                        )
                    },
                    enabled = canModifyNote,
                    onClick = { showMenu = false; onDeleteClick() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.note_menu_note_info)) },
                    onClick = { showMenu = false; onNoteInfoClick() }
                )
            }
        }
    }
}

/**
 * The bottom formatting toolbar: Bold/Italic/Underline/Aa are only functional for a TEXT note's
 * body ([formattingEnabled]) - a CHECKLIST note has no single content field for them to act on,
 * so they're shown but disabled rather than hidden, keeping the toolbar's layout identical across
 * both note types. Bulleted list, Checklist, and both items in the + menu are placeholders only,
 * per this task's own spec.
 */
@Composable
private fun NoteFormattingToolbar(
    formattingEnabled: Boolean,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isUnderlineActive: Boolean,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onSizeSelected: (NoteFormatStyle?) -> Unit,
    onBulletedListClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onAddImageClick: () -> Unit,
    onOpenCameraClick: () -> Unit
) {
    var showSizeMenu by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }
    val smallSizeDescription = stringResource(id = R.string.note_format_size_small)
    val mediumSizeDescription = stringResource(id = R.string.note_format_size_medium)
    val largeSizeDescription = stringResource(id = R.string.note_format_size_large)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // The app opts into edge-to-edge (see MainActivity's enableEdgeToEdge()), so the
            // system's own navigation bar (3-button bar or gesture bar) draws on top of this
            // screen's content unless a screen accounts for it itself - exactly the inset every
            // other screen's own OneTaskBottomNav already consumes via
            // windowInsetsPadding(WindowInsets.navigationBars). This toolbar has no
            // OneTaskBottomNav of its own (the Note Editor is a full-screen editor, not one of
            // the three tab screens), so it must consume that same inset directly, or its own
            // content renders underneath/behind the system bar instead of sitting above it.
            // union(WindowInsets.ime) keeps the toolbar rising above the keyboard when it's open
            // (this screen's previous imePadding()-only behavior) while never ALSO adding the
            // navigation-bar inset on top of that - union is the larger of the two, not their
            // sum, so there's no double-padding either way.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FormatGlyphButton(
                text = "B",
                bold = true,
                active = isBoldActive,
                enabled = formattingEnabled,
                description = stringResource(id = R.string.note_format_bold),
                onClick = onBoldClick
            )
            FormatGlyphButton(
                text = "I",
                italic = true,
                active = isItalicActive,
                enabled = formattingEnabled,
                description = stringResource(id = R.string.note_format_italic),
                onClick = onItalicClick
            )
            FormatGlyphButton(
                text = "U",
                underline = true,
                active = isUnderlineActive,
                enabled = formattingEnabled,
                description = stringResource(id = R.string.note_format_underline),
                onClick = onUnderlineClick
            )

            Box {
                FormatGlyphButton(
                    text = "Aa",
                    active = false,
                    enabled = formattingEnabled,
                    description = stringResource(id = R.string.note_format_text_size),
                    onClick = { showSizeMenu = true }
                )
                DropdownMenu(
                    expanded = showSizeMenu,
                    onDismissRequest = { showSizeMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "A",
                                fontSize = 16.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = smallSizeDescription
                                }
                            )
                        },
                        onClick = { showSizeMenu = false; onSizeSelected(null) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "A",
                                fontSize = 22.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = mediumSizeDescription
                                }
                            )
                        },
                        onClick = { showSizeMenu = false; onSizeSelected(NoteFormatStyle.HEADING_MEDIUM) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "A",
                                fontSize = 28.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = largeSizeDescription
                                }
                            )
                        },
                        onClick = { showSizeMenu = false; onSizeSelected(NoteFormatStyle.HEADING_LARGE) }
                    )
                }
            }

            FormatGlyphButton(
                text = "☰",
                active = false,
                enabled = true,
                description = stringResource(id = R.string.note_format_bulleted_list),
                onClick = onBulletedListClick
            )
            FormatGlyphButton(
                text = "☑",
                active = false,
                enabled = true,
                description = stringResource(id = R.string.note_type_checklist),
                onClick = onChecklistClick
            )

            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { showPlusMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.note_editor_plus_button),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showPlusMenu,
                    onDismissRequest = { showPlusMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.note_plus_menu_add_image)) },
                        onClick = { showPlusMenu = false; onAddImageClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.note_plus_menu_open_camera)) },
                        onClick = { showPlusMenu = false; onOpenCameraClick() }
                    )
                }
            }
        }
    }
}

/** One glyph-style toolbar button - the letter/symbol itself IS the icon (matching the Note
 * Editor's target design), styled bold/italic/underlined to also serve as its own preview of what
 * it does. [active] highlights it (an armed pending style, or a fully-covered selection - see
 * [NoteEditorScreen]'s own isCharacterStyleActive) with the same secondaryContainer/primary
 * treatment [SegmentedTab] in the Timer screen already uses for its own selected state. */
@Composable
private fun FormatGlyphButton(
    text: String,
    active: Boolean,
    enabled: Boolean,
    description: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (bold || active) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
            color = contentColor
        )
    }
}

/**
 * The Note Editor's "Add to Label" picker, backed by the app's existing per-note Label mechanism
 * ([JournalNoteEntity.label]) - Notes intentionally have no relationship to Tasks' own Category
 * system, so this dialog and its wording are entirely Notes/Label-scoped, never "Category".
 * Styled identically to [com.nj031.onetask.ui.components.CategorySelectorDialog] (same
 * Dialog/Surface/row treatment), reusing this app's existing picker visual language rather than
 * inventing a second one. [onAddLabelClick] hands off to the existing Labels screen (the app's
 * one existing "create a new label" flow - see LabelsScreen), the same way
 * CategorySelectorDialog's own "+ Add Category" hands off instead of creating one inline here.
 */
@Composable
private fun NoteLabelSelectorDialog(
    currentLabel: String?,
    labels: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onAddLabelClick: () -> Unit
) {
    var pendingLabel by remember(currentLabel) { mutableStateOf(currentLabel) }

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
                    text = stringResource(id = R.string.note_menu_add_to_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item(key = "no_label") {
                        NoteLabelOptionRow(
                            text = stringResource(id = R.string.note_no_label),
                            selected = pendingLabel == null,
                            onClick = { pendingLabel = null }
                        )
                    }
                    items(labels, key = { it }) { labelName ->
                        NoteLabelOptionRow(
                            text = labelName,
                            selected = pendingLabel == labelName,
                            onClick = { pendingLabel = labelName }
                        )
                    }
                }

                Button(
                    onClick = { onConfirm(pendingLabel) },
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
                    onClick = onAddLabelClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.note_add_label_button),
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
private fun NoteLabelOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
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

private val noteInfoTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())

private fun Long.toNoteInfoTimestampText(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(noteInfoTimestampFormatter)

/** Read-only "Note info" dialog - every value shown comes straight from [note] (or, for
 * title/label, the editor's own live in-progress state) rather than being recomputed, so it can
 * never show anything other than real existing data. Deliberately has no word/character count. */
@Composable
private fun NoteInfoDialog(
    note: JournalNoteEntity,
    title: String,
    noteLabel: String?,
    onDismiss: () -> Unit
) {
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
                    text = stringResource(id = R.string.note_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                NoteInfoRow(
                    label = stringResource(id = R.string.note_info_name_label),
                    value = title.ifBlank { stringResource(id = R.string.note_title_placeholder) }
                )
                NoteInfoRow(
                    label = stringResource(id = R.string.note_info_type_label),
                    value = stringResource(
                        id = when (note.noteType) {
                            JournalNoteType.TEXT -> R.string.note_type_text
                            JournalNoteType.CHECKLIST -> R.string.note_type_checklist
                        }
                    )
                )
                NoteInfoRow(
                    label = stringResource(id = R.string.note_info_created_label),
                    value = note.createdAt.toNoteInfoTimestampText()
                )
                NoteInfoRow(
                    label = stringResource(id = R.string.note_info_modified_label),
                    value = note.updatedAt.toNoteInfoTimestampText()
                )
                NoteInfoRow(
                    label = stringResource(id = R.string.note_info_labels_label),
                    value = noteLabel ?: stringResource(id = R.string.note_no_label)
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.close),
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
private fun NoteInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================
// Formatting-span engine backing Bold/Italic/Underline/Aa - see NoteFormatSpan's own doc comment
// for the data model these operate on. Each function here is a small, self-contained, pure
// transform over a List<NoteFormatSpan>; none of them touch Compose state directly, so they're
// exercised the same way regardless of which control (toolbar tap vs. live typing) drives them.
// ============================================================================

private val HEADING_MEDIUM_FONT_SIZE = 20.sp
private val HEADING_LARGE_FONT_SIZE = 24.sp

/** Builds the [VisualTransformation] that actually renders [spans] as styled text inside the
 * content TextField while it keeps editing plain text underneath - [OffsetMapping.Identity] is
 * exactly correct here since this only ever restyles existing characters, never adds or removes
 * any for display. */
private fun noteFormatVisualTransformation(spans: List<NoteFormatSpan>): VisualTransformation =
    VisualTransformation { text ->
        if (spans.isEmpty()) return@VisualTransformation TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        spans.forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (start >= end) return@forEach
            val spanStyle = when (span.style) {
                NoteFormatStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                NoteFormatStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                NoteFormatStyle.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                NoteFormatStyle.HEADING_MEDIUM -> SpanStyle(fontSize = HEADING_MEDIUM_FONT_SIZE, fontWeight = FontWeight.Bold)
                NoteFormatStyle.HEADING_LARGE -> SpanStyle(fontSize = HEADING_LARGE_FONT_SIZE, fontWeight = FontWeight.Bold)
            }
            builder.addStyle(spanStyle, start, end)
        }
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

/** The single contiguous edit region between [old] and [new] text - the common prefix/suffix
 * diff every real TextField edit (typing, pasting, deleting, autocorrect) produces. [oldStart] is
 * where the edit begins (in both old- and new-text coordinates, since nothing before it moved),
 * [oldEnd] is where it ends in the OLD text, and [newEnd] is where it ends in the NEW text. */
private data class TextEditDiff(val oldStart: Int, val oldEnd: Int, val newEnd: Int) {
    val delta: Int get() = (newEnd - oldStart) - (oldEnd - oldStart)
}

private fun diffText(old: String, new: String): TextEditDiff {
    if (old == new) return TextEditDiff(old.length, old.length, new.length)
    val maxPrefix = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++
    val maxSuffix = maxPrefix - prefix
    var suffix = 0
    while (suffix < maxSuffix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    return TextEditDiff(oldStart = prefix, oldEnd = old.length - suffix, newEnd = new.length - suffix)
}

/** Maps a single offset from old-text to new-text coordinates across [diff]: unchanged before the
 * edit, shifted by [TextEditDiff.delta] after it, and collapsed to [TextEditDiff.oldStart] when it
 * fell strictly inside the replaced region (it has no exact analogue in the new text). */
private fun mapOffsetAcrossEdit(offset: Int, diff: TextEditDiff): Int = when {
    offset <= diff.oldStart -> offset
    offset >= diff.oldEnd -> offset + diff.delta
    else -> diff.oldStart
}

/** Re-maps every span across a text edit - shifting spans after the edit, extending a span that
 * fully contained the edit (so typing inside a bold word keeps it bold), and dropping any span
 * that collapses to nothing (fully inside a deletion). Called on every keystroke, before this
 * edit's own newly-typed characters (if any) pick up [pendingCharacterStyles] - see
 * [NoteEditorScreen]'s content TextField onValueChange. */
private fun shiftSpansForEdit(spans: List<NoteFormatSpan>, diff: TextEditDiff): List<NoteFormatSpan> =
    spans.mapNotNull { span ->
        val newStart = mapOffsetAcrossEdit(span.start, diff)
        val newEnd = mapOffsetAcrossEdit(span.end, diff)
        if (newEnd <= newStart) null else span.copy(start = newStart, end = newEnd)
    }

/** Whether every character in [start, end) already has [style] applied, i.e. whether tapping
 * that style's toolbar button over this exact range should remove it rather than add it. */
private fun isRangeFullyCovered(spans: List<NoteFormatSpan>, style: NoteFormatStyle, start: Int, end: Int): Boolean {
    if (start >= end) return false
    var cursor = start
    for (span in spans.filter { it.style == style }.sortedBy { it.start }) {
        if (span.end <= cursor) continue
        if (span.start > cursor) return false
        cursor = maxOf(cursor, span.end)
        if (cursor >= end) return true
    }
    return cursor >= end
}

/** Adds [style] coverage over [start, end), merging with any same-style spans that already
 * overlap or touch that range into one normalized span, so applying a style twice over
 * overlapping selections never leaves behind redundant/fragmented spans. */
private fun addCoverage(spans: List<NoteFormatSpan>, style: NoteFormatStyle, start: Int, end: Int): List<NoteFormatSpan> {
    if (start >= end) return spans
    val others = spans.filterNot { it.style == style }
    var newStart = start
    var newEnd = end
    val untouched = mutableListOf<NoteFormatSpan>()
    spans.filter { it.style == style }.forEach { span ->
        if (span.end < newStart || span.start > newEnd) {
            untouched += span
        } else {
            newStart = minOf(newStart, span.start)
            newEnd = maxOf(newEnd, span.end)
        }
    }
    return others + untouched + NoteFormatSpan(newStart, newEnd, style)
}

/** Removes [style] coverage over [start, end) - splitting any same-style span that only partially
 * overlaps the removed range so the portion outside it keeps the style, and dropping any span (or
 * the part of one) that falls entirely inside the removed range. */
private fun removeCoverage(spans: List<NoteFormatSpan>, style: NoteFormatStyle, start: Int, end: Int): List<NoteFormatSpan> {
    if (start >= end) return spans
    val others = spans.filterNot { it.style == style }
    val result = mutableListOf<NoteFormatSpan>()
    spans.filter { it.style == style }.forEach { span ->
        when {
            span.end <= start || span.start >= end -> result += span
            else -> {
                if (span.start < start) result += span.copy(end = start)
                if (span.end > end) result += span.copy(start = end)
            }
        }
    }
    return others + result
}

/** Bold/Italic/Underline's toolbar behavior: remove [style] over [start, end) if the whole range
 * already has it, otherwise add it - ordinary toggle semantics for a selection. */
private fun toggleStyleOverRange(spans: List<NoteFormatSpan>, style: NoteFormatStyle, start: Int, end: Int): List<NoteFormatSpan> =
    if (isRangeFullyCovered(spans, style, start, end)) removeCoverage(spans, style, start, end) else addCoverage(spans, style, start, end)

/** The Aa control's behavior: HEADING_MEDIUM and HEADING_LARGE are mutually exclusive, so
 * choosing one first clears the other over [start, end); [style] null (the "Small" / Normal
 * option) just clears both, since there's no explicit NORMAL style to add. */
private fun setHeadingOverRange(spans: List<NoteFormatSpan>, style: NoteFormatStyle?, start: Int, end: Int): List<NoteFormatSpan> {
    var result = removeCoverage(spans, NoteFormatStyle.HEADING_MEDIUM, start, end)
    result = removeCoverage(result, NoteFormatStyle.HEADING_LARGE, start, end)
    return if (style != null) addCoverage(result, style, start, end) else result
}

@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChange: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusTargetId by remember { mutableStateOf<String?>(null) }
    var focusedItemId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Drag-and-drop reorder state, mirroring the Tasks homepage's own drag-and-drop
    // implementation: draggedItemId is non-null only while a long-press-drag is in progress,
    // dragOffsetY is that one item's live, cumulative finger movement in px (applied as a visual
    // translation), and dragPreviewItems is the optimistic, already-swapped order while a drag is
    // in progress - null the rest of the time, in which case [items] (the checklist's real,
    // persisted order) is rendered directly. Rendering [items] directly whenever nothing is being
    // dragged - instead of mirroring it into a separate state asynchronously - matters: an extra
    // frame of lag here was letting a checklist row's text field see its own just-typed value
    // reasserted a moment later as if it came from outside, which was breaking both the first
    // character's cursor placement and the keyboard's Backspace/Delete key-repeat.
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragPreviewItems by remember { mutableStateOf<List<ChecklistItem>?>(null) }
    val renderedItems = if (draggedItemId != null) (dragPreviewItems ?: items) else items

    // Drag auto-scroll: while an item is being dragged and it's within the top/bottom edge zone
    // of the visible list area, keep scrolling that direction every frame - independent of the
    // typing auto-scroll below, and independent of the reorder swap itself (which only fires on
    // finger movement), so holding near an edge keeps the list moving even if the drag itself
    // pauses momentarily.
    LaunchedEffect(draggedItemId) {
        val id = draggedItemId ?: return@LaunchedEffect
        while (isActive) {
            val info = listState.layoutInfo
            val draggedInfo = info.visibleItemsInfo.find { it.key == id }
            if (draggedInfo != null) {
                val draggedTop = draggedInfo.offset + dragOffsetY
                val draggedBottom = draggedTop + draggedInfo.size
                val viewportTop = info.viewportStartOffset
                val viewportBottom = info.viewportEndOffset
                val edgeZone = ((viewportBottom - viewportTop) * 0.18f).coerceAtLeast(1f)
                when {
                    draggedTop < viewportTop + edgeZone -> listState.scrollBy(-14f)
                    draggedBottom > viewportBottom - edgeZone -> listState.scrollBy(14f)
                }
            }
            withFrameNanos { }
        }
    }

    // Typing auto-scroll: whenever the focused item changes, or its (or any item's) content
    // changes - e.g. it wraps onto another line, or a new item was just inserted below it -
    // re-check whether the focused item still fits above the keyboard/viewport bottom and scroll
    // up just enough if not. Mirrors the Text Note editor's own "keep the active line visible"
    // backstop and the Tasks homepage's subtask-expand auto-scroll: wait a couple of frames for
    // the new layout to settle, then scroll only the minimum necessary amount.
    LaunchedEffect(focusedItemId, renderedItems) {
        val targetId = focusedItemId ?: return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.find { it.key == targetId } ?: return@LaunchedEffect
        val overflow = (item.offset + item.size) - info.viewportEndOffset
        if (overflow > 0) {
            val maxScroll = (item.offset - info.viewportStartOffset).coerceAtLeast(0)
            val scrollAmount = overflow.coerceAtMost(maxScroll).toFloat()
            if (scrollAmount > 0f) {
                listState.animateScrollBy(scrollAmount)
            }
        }
    }

    LazyColumn(state = listState, modifier = modifier) {
        items(renderedItems, key = { it.id }) { item ->
            val isDragged = item.id == draggedItemId
            ChecklistItemRow(
                item = item,
                requestFocus = focusTargetId == item.id,
                onFocusHandled = { focusTargetId = null },
                onFocusChanged = { focused -> if (focused) focusedItemId = item.id },
                onCheckedChange = { checked ->
                    onItemsChange(items.map { if (it.id == item.id) it.copy(checked = checked) else it })
                },
                onTextChange = { text ->
                    onItemsChange(items.map { if (it.id == item.id) it.copy(text = text) else it })
                },
                onDeleteClick = {
                    onItemsChange(items.filterNot { it.id == item.id })
                },
                onEnterPressed = {
                    // Empty item protection: Enter on a row that's still blank does nothing,
                    // rather than piling up more blank rows underneath it.
                    val currentIndex = items.indexOfFirst { it.id == item.id }
                    val current = items.getOrNull(currentIndex)
                    if (currentIndex >= 0 && current != null && current.text.isNotBlank()) {
                        val newItem = ChecklistItem(text = "")
                        val newList = items.toMutableList().apply { add(currentIndex + 1, newItem) }
                        onItemsChange(newList)
                        focusTargetId = newItem.id
                    }
                },
                isDragged = isDragged,
                dragOffsetY = if (isDragged) dragOffsetY else 0f,
                onDragStart = {
                    draggedItemId = item.id
                    dragOffsetY = 0f
                },
                onDrag = { deltaY ->
                    dragOffsetY += deltaY
                    // Live reads of dragPreviewItems here (not a value captured when this
                    // closure was created) matter: several onDrag calls can land before
                    // recomposition catches up, and each one must see the previous one's result.
                    val base = dragPreviewItems ?: items
                    val (reordered, correctedOffset) = checklistDragSwapIfNeeded(
                        items = base,
                        draggedItemId = item.id,
                        dragOffsetY = dragOffsetY,
                        listState = listState
                    )
                    dragPreviewItems = reordered
                    dragOffsetY = correctedOffset
                },
                onDragEnd = {
                    val finalOrder = dragPreviewItems ?: items
                    draggedItemId = null
                    dragPreviewItems = null
                    dragOffsetY = 0f
                    onItemsChange(finalOrder)
                }
            )
        }

        item(key = "__add_checklist_item__") {
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
}

/**
 * Finds whether the dragged item has crossed far enough past a visible neighbor to swap places
 * with it - the exact same "compare dragged center to neighbor center, swap, and carry over the
 * neighbor's height as an offset correction" approach the Tasks homepage's own drag-and-drop
 * (dragSwapIfNeeded) already uses, so a long-press-drag on a checklist item behaves identically.
 * A no-op (returns the inputs unchanged) once this frame's crossing doesn't warrant a swap, or if
 * layout info for the relevant items isn't available yet (e.g. scrolled just out of view).
 */
private fun checklistDragSwapIfNeeded(
    items: List<ChecklistItem>,
    draggedItemId: String,
    dragOffsetY: Float,
    listState: LazyListState
): Pair<List<ChecklistItem>, Float> {
    val draggedIndex = items.indexOfFirst { it.id == draggedItemId }
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val draggedInfo = visibleItems.find { it.key == draggedItemId }
    if (draggedIndex < 0 || draggedInfo == null) return items to dragOffsetY
    val draggedCenter = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY

    val nextInfo = items.getOrNull(draggedIndex + 1)?.let { next -> visibleItems.find { it.key == next.id } }
    if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size / 2f) {
        val reordered = items.toMutableList().apply { add(draggedIndex + 1, removeAt(draggedIndex)) }
        return reordered to (dragOffsetY - nextInfo.size)
    }

    val prevInfo = items.getOrNull(draggedIndex - 1)?.let { prev -> visibleItems.find { it.key == prev.id } }
    if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size / 2f) {
        val reordered = items.toMutableList().apply { add(draggedIndex - 1, removeAt(draggedIndex)) }
        return reordered to (dragOffsetY + prevInfo.size)
    }

    return items to dragOffsetY
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterPressed: () -> Unit,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val focusRequester = remember(item.id) { FocusRequester() }

    // Owns this row's live text + cursor/selection locally, seeded once (per item id) from the
    // persisted text. The field used to be driven directly by `item.text: String`, which lets
    // Compose's TextField reconstruct its own internal selection state from scratch on every
    // recompose - that reconstruction was placing the cursor before, not after, a newly typed
    // first character, and was also interrupting the platform keyboard's Backspace/Delete
    // key-repeat (each keystroke's round trip back through the checklist's own state and back
    // down as a new `item.text` was enough to make the field look like it had been changed from
    // outside). Owning a real TextFieldValue locally - the same pattern already used correctly by
    // this screen's Text Note content field - keeps the field's cursor/selection continuous
    // across keystrokes; edits still flow up to the checklist's own state via onTextChange.
    var textFieldValue by remember(item.id) {
        mutableStateOf(TextFieldValue(text = item.text, selection = TextRange(item.text.length)))
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer { translationY = dragOffsetY },
        shape = RoundedCornerShape(10.dp),
        color = if (isDragged) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shadowElevation = if (isDragged) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                ChecklistCheckbox(checked = item.checked, onToggle = { onCheckedChange(!item.checked) })
            }

            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != item.text) {
                        onTextChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.checklist_item_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                // Deliberately no strike-through/decoration on the text regardless of [checked] -
                // completed checklist items look exactly like unchecked ones apart from the box.
                // singleLine is intentionally false so long text wraps onto further lines instead
                // of scrolling off-screen, and the row's height grows to fit; Enter is still never
                // typed as a literal newline into the text because imeAction is Next below, not
                // Default - Compose routes Enter to onNext instead of inserting "\n" whenever a
                // non-default imeAction is set, even in multiline fields.
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { onEnterPressed() }),
                colors = transparentTextFieldColors()
            )

            // A fixed-size trailing slot for the delete button - it's an unweighted sibling of
            // the weighted text field above, so Row always reserves its width first and the text
            // can never grow underneath or behind it, at any wrap length.
            IconButton(onClick = onDeleteClick, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // A dedicated drag handle, separate from the text field above: the text field needs
            // first claim on every touch that starts on it (to place the cursor, select text, or
            // just type), so a long-press-drag gesture spanning the whole row is never actually
            // reachable when the row is mostly text field. Long-pressing this handle instead lifts
            // the item into the same dragged state as before (background, elevation, and
            // translationY are all still applied to the whole Surface above, so the entire
            // multi-line item still moves as one unit) and reuses the exact same swap/auto-scroll
            // logic the Tasks homepage's own drag-and-drop already uses.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(40.dp)
                    .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                ChecklistDragHandle()
            }
        }
    }
}

/** A small grip icon (three horizontal bars) marking the checklist item's long-press-to-drag
 * touch target - drawn directly, matching this file's existing stroke-based custom icon style,
 * since the app doesn't depend on Material's extended icon pack. */
@Composable
private fun ChecklistDragHandle(tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = size.minDimension * 0.12f
        val left = size.width * 0.15f
        val right = size.width * 0.85f
        listOf(0.24f, 0.5f, 0.76f).forEach { fraction ->
            val y = size.height * fraction
            drawLine(
                color = tint,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
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
