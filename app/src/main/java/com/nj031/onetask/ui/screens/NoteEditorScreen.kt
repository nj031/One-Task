package com.nj031.onetask.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteEntity
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.journal.NoteBlock
import com.nj031.onetask.data.journal.NoteBlockType
import com.nj031.onetask.data.journal.NoteFormatSpan
import com.nj031.onetask.data.journal.NoteFormatStyle
import com.nj031.onetask.data.journal.NoteImageStorage
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.viewmodel.JournalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * The single mixed-content editor for every note - a note is an ordered list of [NoteBlock]s
 * ([EditableBlock] while being edited here), so normal text, bullet items, checklist items, and
 * (up to one, for now) an image can coexist in any order within one note. See [NoteBlock]'s own
 * doc comment for the persisted data model, and [legacyBlocksFor] for how a note saved before this
 * editor existed is read.
 *
 * State here deliberately uses plain `remember`, not `rememberSaveable`: the app going to the
 * background or the screen locking never destroys this composition (Activity.onStop, not
 * onDestroy), so `remember` alone already preserves in-progress edits across both - exactly the
 * "preserve on background/lock, but not across a real process kill" behavior this needs, with no
 * extra plumbing. The same is true of navigating to Labels and back (see [onManageLabelsClick]
 * below) - it's a normal back-stack push, not a recreation, so this composition (and every var
 * below) survives the round trip unchanged.
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
    // even if both somehow ever fired (they can't produce two notes for one editing session). It
    // also doubles as the deterministic key NoteImageStorage/CloudBackupRepository's own note-
    // image Storage path use, so an image can be saved before the note itself is confirmed
    // persisted.
    val pendingNoteId = remember(noteId) { noteId ?: UUID.randomUUID().toString() }
    // The note's real, currently-persisted DB row - null for a brand-new note until it first gets
    // real content (see the hasContentNow LaunchedEffect below). Distinct from [existingNote],
    // which is captured once at composition entry and never updates: Pin/Archive/Delete and Note
    // Info must react the moment a new note becomes real, without needing the editor to be closed
    // and reopened (which is what used to create a fresh, non-null existingNote instead).
    var persistedNote by remember(noteId) { mutableStateOf(existingNote) }

    var title by remember { mutableStateOf(existingNote?.title.orEmpty()) }
    var blocks by remember { mutableStateOf(initialBlocksFor(existingNote)) }
    var noteLabel by remember { mutableStateOf(existingNote?.label) }
    var isPinned by remember { mutableStateOf(existingNote?.pinned ?: false) }
    // Armed while the focused block's selection is collapsed: the next characters typed inherit
    // whichever of these styles are armed (see onBlockValueChange below) - the "start applying
    // from the current cursor/typing position" behavior Bold/Italic/Underline/Aa need when
    // nothing is selected, mirroring an ordinary word processor's own "type in Bold" behavior.
    var pendingCharacterStyles by remember { mutableStateOf(emptySet<NoteFormatStyle>()) }
    // Which block currently owns keyboard focus - never cleared on blur (only ever set on focus
    // gain), so it stays "sticky" to the last focused block through a transient toolbar tap,
    // mirroring the same pattern this screen's own checklist rows already used before this
    // rewrite. Used both for routing B/I/U/Aa/Bullet/Checklist toolbar actions and as the
    // insertion point for Add Image/Open Camera.
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    // Non-null while a newly-inserted block (from pressing Enter, or converting a line to
    // Bullet/Checklist) should claim keyboard focus on its next composition - mirrors this
    // screen's own pre-existing checklist "requestFocus" pattern.
    var focusTargetId by remember { mutableStateOf<String?>(null) }
    // Non-null while the tapped image is shown full-screen (see ImageFocusOverlay) - see this
    // task's own spec for the required Back/Delete behavior.
    var focusedImageBlockId by remember { mutableStateOf<String?>(null) }

    var showLabelDialog by remember { mutableStateOf(false) }
    var showNoteInfoDialog by remember { mutableStateOf(false) }
    val labels by viewModel.labels.collectAsState()
    val context = LocalContext.current

    fun updateBlock(id: String, transform: (EditableBlock) -> EditableBlock) {
        blocks = blocks.map { if (it.id == id) transform(it) else it }
    }

    // A note only "has content" worth keeping when the title or at least one block is non-blank -
    // an empty block created just by opening the editor doesn't count on its own, matching how an
    // untouched note (title blank, one empty TEXT block) never saves.
    fun hasContent(): Boolean = title.isNotBlank() || blocks.any { block ->
        if (block.type == NoteBlockType.IMAGE) true else block.value.text.isNotBlank()
    }

    fun commitOnExit() {
        val note = persistedNote
        if (hasContent()) {
            val persistedBlocks = blocks.map { it.toPersisted() }
            if (note != null) {
                viewModel.updateNote(
                    note = note,
                    title = title,
                    content = persistedBlocks.toLegacyContent(),
                    checklistItems = persistedBlocks.toLegacyChecklistItems(),
                    label = noteLabel,
                    contentFormatSpans = emptyList(),
                    // Re-asserts whatever togglePin() already wrote immediately (see its own
                    // comment) rather than reverting it - note (the composition-time snapshot)
                    // never itself observes a pin toggle that happened after this screen opened.
                    pinned = isPinned,
                    blocks = persistedBlocks
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
                    content = persistedBlocks.toLegacyContent(),
                    noteType = effectiveNoteType,
                    checklistItems = persistedBlocks.toLegacyChecklistItems(),
                    label = noteLabel,
                    contentFormatSpans = emptyList(),
                    pinned = isPinned,
                    id = pendingNoteId,
                    blocks = persistedBlocks
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
        val body = blocks.map { it.toPersisted() }.toLegacyContent()
        val shareText = if (title.isNotBlank()) "$title\n\n$body" else body
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    // The block B/I/U/Aa/Bullet/Checklist/Add Image/Open Camera act on: the focused one, or (if
    // nothing is currently focused - e.g. right after opening the note) the last block, a
    // reasonable "continue where you left off" default.
    val targetBlock: EditableBlock? = blocks.find { it.id == focusedBlockId } ?: blocks.lastOrNull()
    val formattingEnabled = targetBlock != null && targetBlock.type != NoteBlockType.IMAGE

    fun isCharacterStyleActive(style: NoteFormatStyle): Boolean {
        val block = targetBlock ?: return false
        val sel = block.value.selection
        return if (sel.collapsed) style in pendingCharacterStyles else isRangeFullyCovered(block.formatSpans, style, sel.min, sel.max)
    }

    fun toggleCharacterStyle(style: NoteFormatStyle) {
        val block = targetBlock ?: return
        val sel = block.value.selection
        if (!sel.collapsed) {
            updateBlock(block.id) { it.copy(formatSpans = toggleStyleOverRange(it.formatSpans, style, sel.min, sel.max)) }
        } else {
            pendingCharacterStyles = if (style in pendingCharacterStyles) pendingCharacterStyles - style else pendingCharacterStyles + style
        }
    }

    fun applyHeading(style: NoteFormatStyle?) {
        val block = targetBlock ?: return
        val sel = block.value.selection
        if (!sel.collapsed) {
            updateBlock(block.id) { it.copy(formatSpans = setHeadingOverRange(it.formatSpans, style, sel.min, sel.max)) }
        } else {
            // No selection: mirrors Bold/Italic/Underline's own pendingCharacterStyles behavior
            // above - arms the chosen size for the NEXT characters typed at the cursor rather
            // than retroactively resizing whatever's already on the current line.
            pendingCharacterStyles = pendingCharacterStyles - NoteFormatStyle.HEADING_MEDIUM - NoteFormatStyle.HEADING_LARGE
            if (style != null) {
                pendingCharacterStyles = pendingCharacterStyles + style
            }
        }
    }

    fun onBlockValueChange(block: EditableBlock, newValue: TextFieldValue) {
        val diff = diffText(block.value.text, newValue.text)
        var updatedSpans = shiftSpansForEdit(block.formatSpans, diff)
        if (pendingCharacterStyles.isNotEmpty() && diff.newEnd > diff.oldStart) {
            pendingCharacterStyles.forEach { style ->
                updatedSpans = addCoverage(updatedSpans, style, diff.oldStart, diff.newEnd)
            }
        }
        updateBlock(block.id) { it.copy(value = newValue, formatSpans = updatedSpans) }
    }

    // Enter inside a BULLET/CHECKLIST block (routed here via imeAction=Next, never as a literal
    // "\n" - see EditableBlockRow's own comment): a non-empty item gets a new, same-type empty
    // item right after it (focus follows); an empty item instead exits back to normal text, in
    // place, rather than piling up empty items indefinitely.
    fun handleBlockEnterPressed(blockId: String) {
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val block = blocks[index]
        if (block.value.text.isBlank()) {
            blocks = blocks.mapIndexed { i, b -> if (i == index) b.copy(type = NoteBlockType.TEXT, checked = false) else b }
        } else {
            val newBlock = EditableBlock(id = UUID.randomUUID().toString(), type = block.type)
            blocks = blocks.toMutableList().apply { add(index + 1, newBlock) }
            focusTargetId = newBlock.id
        }
    }

    // Backspace at the very start of an already-empty BULLET/CHECKLIST block: removes it outright
    // (never leaving the note with zero blocks - a lone empty TEXT block takes its place if it
    // was the only one). Backspace at the very start of a non-empty BULLET/CHECKLIST block:
    // converts it back to a plain TEXT block in place, keeping its text and cursor position,
    // rather than merging text across blocks (safer/simpler, and still natural editor behavior).
    // Best-effort: Android soft keyboards don't always deliver a KeyEvent for backspace at an
    // empty field the way a hardware key does - see EditableBlockRow's own onKeyEvent comment.
    fun handleBlockBackspaceAtStart(blockId: String) {
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val block = blocks[index]
        if (block.value.text.isEmpty()) {
            val remaining = blocks.toMutableList().apply { removeAt(index) }
            blocks = remaining.ifEmpty { listOf(EditableBlock(id = UUID.randomUUID().toString(), type = NoteBlockType.TEXT)) }
            focusTargetId = remaining.getOrNull((index - 1).coerceAtLeast(0))?.id
        } else {
            updateBlock(block.id) { it.copy(type = NoteBlockType.TEXT, checked = false) }
        }
    }

    // Bullet/Checklist toolbar buttons: on a TEXT block, splits the selected line(s) (or just the
    // current line/cursor position if nothing is selected - see linesTouchedBy) out into their
    // own new block(s) of [targetType], carrying over whichever formatting spans overlapped each
    // extracted line. On an already-BULLET/CHECKLIST block, converts it in place to the other
    // type, or back to TEXT if it's already [targetType] (a toggle). No-ops on an IMAGE block.
    fun convertFocusedLinesToType(targetType: NoteBlockType) {
        val block = targetBlock ?: return
        when {
            block.type == targetType -> {
                if (block.type != NoteBlockType.TEXT) {
                    updateBlock(block.id) { it.copy(type = NoteBlockType.TEXT, checked = false) }
                }
            }
            block.type == NoteBlockType.BULLET || block.type == NoteBlockType.CHECKLIST -> {
                updateBlock(block.id) { it.copy(type = targetType, checked = false) }
            }
            block.type == NoteBlockType.TEXT -> {
                val text = block.value.text
                val sel = block.value.selection
                val lineRanges = linesTouchedBy(text, sel.start, sel.end)
                if (lineRanges.isEmpty()) return
                val rangeStart = lineRanges.first().first
                val rangeEndExclusive = (lineRanges.last().let { if (it.isEmpty()) it.first else it.last + 1 }).coerceIn(rangeStart, text.length)
                val beforeText = text.substring(0, rangeStart)
                val afterText = text.substring(rangeEndExclusive)

                val newBlocks = mutableListOf<EditableBlock>()
                if (beforeText.isNotEmpty()) {
                    newBlocks += EditableBlock(
                        id = UUID.randomUUID().toString(),
                        type = NoteBlockType.TEXT,
                        value = TextFieldValue(beforeText),
                        formatSpans = rebaseSpans(block.formatSpans, 0, rangeStart)
                    )
                }
                lineRanges.forEach { range ->
                    val lineText = if (range.isEmpty()) "" else text.substring(range.first, range.last + 1)
                    val spanEnd = if (range.isEmpty()) range.first else range.last + 1
                    newBlocks += EditableBlock(
                        id = UUID.randomUUID().toString(),
                        type = targetType,
                        value = TextFieldValue(lineText, selection = TextRange(lineText.length)),
                        formatSpans = rebaseSpans(block.formatSpans, range.first, spanEnd)
                    )
                }
                if (afterText.isNotEmpty()) {
                    newBlocks += EditableBlock(
                        id = UUID.randomUUID().toString(),
                        type = NoteBlockType.TEXT,
                        value = TextFieldValue(afterText),
                        formatSpans = rebaseSpans(block.formatSpans, rangeEndExclusive, text.length)
                    )
                }
                if (newBlocks.isEmpty()) {
                    newBlocks += EditableBlock(id = UUID.randomUUID().toString(), type = targetType)
                }

                val index = blocks.indexOfFirst { it.id == block.id }
                blocks = blocks.toMutableList().apply {
                    removeAt(index)
                    addAll(index, newBlocks)
                }
                focusTargetId = newBlocks.lastOrNull { it.type == targetType }?.id
            }
            // IMAGE - no-op.
        }
    }

    val hasImage = blocks.any { it.type == NoteBlockType.IMAGE }

    // Inserts a new IMAGE block at the current position: splitting the focused TEXT block at its
    // cursor (so text before/after the cursor stays as its own TEXT block on either side of the
    // image, per this task's own spec), or immediately after the focused block if it isn't TEXT,
    // or at the very end if nothing is focused.
    fun insertImageBlock(imagePath: String) {
        val imageBlock = EditableBlock(id = UUID.randomUUID().toString(), type = NoteBlockType.IMAGE, imagePath = imagePath)
        val block = targetBlock
        if (block == null) {
            blocks = blocks + imageBlock
            return
        }
        if (block.type == NoteBlockType.TEXT) {
            val text = block.value.text
            val cursor = block.value.selection.start.coerceIn(0, text.length)
            val before = text.substring(0, cursor)
            val after = text.substring(cursor)
            val newBlocks = mutableListOf<EditableBlock>()
            if (before.isNotEmpty()) {
                newBlocks += EditableBlock(
                    id = UUID.randomUUID().toString(),
                    type = NoteBlockType.TEXT,
                    value = TextFieldValue(before),
                    formatSpans = rebaseSpans(block.formatSpans, 0, cursor)
                )
            }
            newBlocks += imageBlock
            if (after.isNotEmpty()) {
                newBlocks += EditableBlock(
                    id = UUID.randomUUID().toString(),
                    type = NoteBlockType.TEXT,
                    value = TextFieldValue(after),
                    formatSpans = rebaseSpans(block.formatSpans, cursor, text.length)
                )
            }
            val index = blocks.indexOfFirst { it.id == block.id }
            blocks = blocks.toMutableList().apply {
                removeAt(index)
                addAll(index, newBlocks)
            }
        } else {
            val index = blocks.indexOfFirst { it.id == block.id }
            blocks = blocks.toMutableList().apply { add(index + 1, imageBlock) }
        }
    }

    fun deleteImageBlock(blockId: String) {
        blocks = blocks.filterNot { it.id == blockId }
        focusedImageBlockId = null
        NoteImageStorage.delete(context, pendingNoteId)
    }

    val addImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val savedPath = NoteImageStorage.saveFrom(context, pendingNoteId, uri)
            if (savedPath != null) insertImageBlock(savedPath)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        // The captured photo (on success) is already at NoteImageStorage.fileFor(pendingNoteId) -
        // the exact Uri passed to launch() below points straight at that final local file, so
        // there's no extra copy step. On cancel/failure, nothing was inserted and that file (if
        // the camera app happened to create an empty one) simply isn't referenced by any block.
        if (success) {
            insertImageBlock(NoteImageStorage.fileFor(context, pendingNoteId).absolutePath)
        }
    }

    fun launchAddImage() {
        addImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchCamera() {
        cameraLauncher.launch(NoteImageStorage.createCaptureUri(context, pendingNoteId))
    }

    // Eagerly persists a brand-new note the moment it first gets real content, instead of only
    // on exit (commitOnExit's own create path) - Pin/Archive/Delete and Note Info all need a real
    // DB row (persistedNote) to act on, and previously had none until the editor was closed and
    // reopened. Keyed on the boolean itself (not on title/blocks), so it fires exactly once per
    // editing session, on the false->true edge - keying on the raw content would restart (and so
    // never let complete) this coroutine on every keystroke typed before the previous attempt's
    // local Room insert finished.
    val hasContentNow = hasContent()
    LaunchedEffect(hasContentNow) {
        if (hasContentNow && persistedNote == null) {
            persistedNote = viewModel.createNoteAwait(
                title = title,
                content = blocks.map { it.toPersisted() }.toLegacyContent(),
                noteType = effectiveNoteType,
                checklistItems = blocks.map { it.toPersisted() }.toLegacyChecklistItems(),
                label = noteLabel,
                contentFormatSpans = emptyList(),
                pinned = isPinned,
                id = pendingNoteId,
                blocks = blocks.map { it.toPersisted() }
            )
        }
    }

    // Restores this note's image from cloud Storage the first time it's opened on a device that
    // doesn't have the local file yet (a fresh install/reinstall, or a note whose image was added
    // on another device) - mirrors AuthViewModel's own downloadProfilePhoto usage, just triggered
    // by opening the note rather than by signing in, since eagerly downloading every note's image
    // for every note during sign-in sync would be far more than this feature needs.
    LaunchedEffect(existingNote?.id) {
        val note = existingNote ?: return@LaunchedEffect
        val hasImageBlock = note.blocks.any { it.type == NoteBlockType.IMAGE } ||
            blocks.any { it.type == NoteBlockType.IMAGE && it.imagePath == null }
        if (!hasImageBlock) return@LaunchedEffect
        val localFile = NoteImageStorage.fileFor(context, note.id)
        if (localFile.exists()) return@LaunchedEffect
        val downloaded = CloudBackupRepository.downloadNoteImage(note.id, localFile)
        if (downloaded) {
            blocks = blocks.map { b -> if (b.type == NoteBlockType.IMAGE) b.copy(imagePath = localFile.absolutePath) else b }
        }
    }

    BackHandler {
        commitOnExit()
        onDone()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NoteFormattingToolbar(
                    formattingEnabled = formattingEnabled,
                    isBoldActive = isCharacterStyleActive(NoteFormatStyle.BOLD),
                    isItalicActive = isCharacterStyleActive(NoteFormatStyle.ITALIC),
                    isUnderlineActive = isCharacterStyleActive(NoteFormatStyle.UNDERLINE),
                    onBoldClick = { toggleCharacterStyle(NoteFormatStyle.BOLD) },
                    onItalicClick = { toggleCharacterStyle(NoteFormatStyle.ITALIC) },
                    onUnderlineClick = { toggleCharacterStyle(NoteFormatStyle.UNDERLINE) },
                    onSizeSelected = { style -> applyHeading(style) },
                    onBulletedListClick = { convertFocusedLinesToType(NoteBlockType.BULLET) },
                    onChecklistClick = { convertFocusedLinesToType(NoteBlockType.CHECKLIST) },
                    imageActionsEnabled = !hasImage,
                    onAddImageClick = ::launchAddImage,
                    onOpenCameraClick = ::launchCamera
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // innerPadding.bottom already reflects the keyboard: NoteFormattingToolbar
                    // (this Scaffold's bottomBar) applies
                    // windowInsetsPadding(navigationBars.union(ime)) to itself, so its own
                    // measured height - which Scaffold uses to compute innerPadding - already
                    // grows by the IME's height while the keyboard is open. Consuming
                    // innerPadding alone is enough - the IME inset only needs to be accounted for
                    // once in this chain (see the prior fix for this screen's own double-inset
                    // bug).
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
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

                    NoteBlocksList(
                        blocks = blocks,
                        focusTargetId = focusTargetId,
                        onFocusTargetHandled = { focusTargetId = null },
                        onFocusChanged = { id -> focusedBlockId = id },
                        onBlockValueChange = ::onBlockValueChange,
                        onCheckedChange = { id, checked -> updateBlock(id) { it.copy(checked = checked) } },
                        onEnterPressed = ::handleBlockEnterPressed,
                        onBackspaceAtStart = ::handleBlockBackspaceAtStart,
                        onDeleteBlock = { id ->
                            val remaining = blocks.filterNot { it.id == id }
                            blocks = remaining.ifEmpty { listOf(EditableBlock(id = UUID.randomUUID().toString(), type = NoteBlockType.TEXT)) }
                        },
                        onImageTap = { id -> focusedImageBlockId = id },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        val focusedImageBlock = blocks.find { it.id == focusedImageBlockId }
        val focusedImagePath = focusedImageBlock?.imagePath
        if (focusedImageBlock != null && focusedImagePath != null) {
            ImageFocusOverlay(
                imagePath = focusedImagePath,
                onBack = { focusedImageBlockId = null },
                onDelete = { deleteImageBlock(focusedImageBlock.id) }
            )
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
 * The bottom formatting toolbar: Bold/Italic/Underline/Aa act on whichever block currently has
 * focus (or the last block, if none does - see [NoteEditorScreen]'s own targetBlock), disabled
 * only while that block is an image ([formattingEnabled]). Bullet/Checklist stay directly on the
 * toolbar (never moved into the + menu); Add Image/Open Camera live in the + menu and are
 * disabled once the note already has an image ([imageActionsEnabled]) - see this task's own
 * one-image-per-note limit.
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
    imageActionsEnabled: Boolean,
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
            // while never ALSO adding the navigation-bar inset on top of that - union is the
            // larger of the two, not their sum, so there's no double-padding either way.
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
                    val imageActionColor = if (imageActionsEnabled) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.note_plus_menu_add_image), color = imageActionColor) },
                        enabled = imageActionsEnabled,
                        onClick = { showPlusMenu = false; onAddImageClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.note_plus_menu_open_camera), color = imageActionColor) },
                        enabled = imageActionsEnabled,
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
// Mixed-content block model (UI-editing layer) - see NoteBlock's own doc comment for the
// persisted data model this mirrors. EditableBlock swaps NoteBlock's plain `text: String` for a
// TextFieldValue, so each block's own live cursor/selection can be tracked (needed for
// Bold/Italic/Underline/Aa and for the Bullet/Checklist line-conversion below) - exactly the same
// reason this screen's old single content field, and its checklist item rows, already owned a
// TextFieldValue of their own rather than a plain String.
// ============================================================================

private data class EditableBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: NoteBlockType,
    val value: TextFieldValue = TextFieldValue(""),
    val checked: Boolean = false,
    val formatSpans: List<NoteFormatSpan> = emptyList(),
    val imagePath: String? = null
)

private fun NoteBlock.toEditable(): EditableBlock = EditableBlock(
    id = id,
    type = type,
    value = TextFieldValue(text, selection = TextRange(text.length)),
    checked = checked,
    formatSpans = formatSpans,
    imagePath = imagePath
)

private fun EditableBlock.toPersisted(): NoteBlock = NoteBlock(
    id = id,
    type = type,
    text = value.text,
    checked = checked,
    formatSpans = formatSpans,
    imagePath = imagePath
)

/** Synthesizes an equivalent block list from a note saved before the mixed-content editor existed
 * (its own [JournalNoteEntity.blocks] is empty) - a TEXT note becomes one TEXT block carrying its
 * old content/contentFormatSpans, a CHECKLIST note becomes one CHECKLIST block per old checklist
 * item (state/order preserved). Purely an in-memory adaptation for this editing session; nothing
 * is written back until the note is actually saved again. */
private fun legacyBlocksFor(note: JournalNoteEntity): List<EditableBlock> = when (note.noteType) {
    JournalNoteType.TEXT -> listOf(
        EditableBlock(
            type = NoteBlockType.TEXT,
            value = TextFieldValue(note.content, selection = TextRange(note.content.length)),
            formatSpans = note.contentFormatSpans
        )
    )
    JournalNoteType.CHECKLIST -> note.checklistItems.map { item ->
        EditableBlock(
            id = item.id,
            type = NoteBlockType.CHECKLIST,
            value = TextFieldValue(item.text, selection = TextRange(item.text.length)),
            checked = item.checked
        )
    }.ifEmpty { listOf(EditableBlock(type = NoteBlockType.TEXT)) }
}

private fun initialBlocksFor(note: JournalNoteEntity?): List<EditableBlock> = when {
    note == null -> listOf(EditableBlock(type = NoteBlockType.TEXT))
    note.blocks.isNotEmpty() -> note.blocks.map { it.toEditable() }
    else -> legacyBlocksFor(note)
}

/** Flattens the note's blocks into one plain-text preview/search string - purely a backward-
 * compatibility bridge so the Notes list's existing card preview and search (JournalViewModel's
 * own matchesSearch), which both still read [JournalNoteEntity.content]/[JournalNoteEntity.status]
 * for a TEXT-type note, keep working unchanged for a mixed-content note without either of them
 * needing to know about [NoteBlock] at all. Never re-parsed back into blocks - [NoteBlock] is
 * always the source of truth once a note has any. */
private fun List<NoteBlock>.toLegacyContent(): String =
    filter { it.type != NoteBlockType.IMAGE }
        .joinToString("\n") { block ->
            when (block.type) {
                NoteBlockType.BULLET -> "• ${block.text}"
                NoteBlockType.CHECKLIST -> "${if (block.checked) "☑" else "☐"} ${block.text}"
                else -> block.text
            }
        }

/** Mirrors [toLegacyContent] for a CHECKLIST-type note's own preview/search path (which reads
 * [JournalNoteEntity.checklistItems] instead of content - see matchesSearch). */
private fun List<NoteBlock>.toLegacyChecklistItems(): List<ChecklistItem> =
    filter { it.type == NoteBlockType.CHECKLIST }
        .map { ChecklistItem(id = it.id, text = it.text, checked = it.checked) }

/** Every line [start, end) touched by [selStart, selEnd] (a collapsed selection touches just the
 * one line the cursor sits on), extended outward to the nearest newlines on either side - the
 * Bullet/Checklist toolbar buttons' own "selected lines, or just the current line" behavior (see
 * this task's own spec). Ranges are in the original [text]'s own coordinates, using the same
 * [start, end) convention as [NoteFormatSpan]. */
private fun linesTouchedBy(text: String, selStart: Int, selEnd: Int): List<IntRange> {
    val lo = selStart.coerceIn(0, text.length)
    val hi = selEnd.coerceIn(lo, text.length)
    var rangeStart = lo
    while (rangeStart > 0 && text[rangeStart - 1] != '\n') rangeStart--
    var rangeEnd = hi
    while (rangeEnd < text.length && text[rangeEnd] != '\n') rangeEnd++
    val segment = text.substring(rangeStart, rangeEnd)
    val lines = segment.split('\n')
    val result = mutableListOf<IntRange>()
    var cursor = rangeStart
    for (line in lines) {
        result += cursor until (cursor + line.length)
        cursor += line.length + 1
    }
    return result
}

/** Extracts whichever spans overlap [rangeStart, rangeEnd), clipped to that range and re-based to
 * be relative to [rangeStart] (0-based within the extracted substring) - how a line's own
 * formatting survives being split out into its own new block (see convertFocusedLinesToType/
 * insertImageBlock in [NoteEditorScreen]). */
private fun rebaseSpans(spans: List<NoteFormatSpan>, rangeStart: Int, rangeEnd: Int): List<NoteFormatSpan> =
    spans.mapNotNull { span ->
        val start = maxOf(span.start, rangeStart)
        val end = minOf(span.end, rangeEnd)
        if (start >= end) null else span.copy(start = start - rangeStart, end = end - rangeStart)
    }

// ============================================================================
// Formatting-span engine backing Bold/Italic/Underline/Aa - see NoteFormatSpan's own doc comment
// for the data model these operate on. Each function here is a small, self-contained, pure
// transform over a List<NoteFormatSpan>; none of them touch Compose state directly, so they're
// exercised the same way regardless of which control (toolbar tap vs. live typing) drives them,
// and regardless of which block's own formatSpans they're called with.
// ============================================================================

private val HEADING_MEDIUM_FONT_SIZE = 20.sp
private val HEADING_LARGE_FONT_SIZE = 24.sp

/** Builds the [VisualTransformation] that actually renders [spans] as styled text inside a block's
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
 * [NoteEditorScreen]'s own onBlockValueChange. */
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

// ============================================================================
// Block rendering
// ============================================================================

/**
 * Renders [blocks] in order inside a LazyColumn - TEXT as plain (multi-line-capable) text,
 * BULLET/CHECKLIST as one-line rows with their own leading marker, IMAGE as an inline picture.
 * Drag-and-drop reordering was intentionally not carried over from this screen's old, pure-
 * checklist editor: ordering here is controlled by WHERE content is inserted (cursor position,
 * Enter-key sequencing, image insertion-at-cursor), which is what this task's own spec's examples
 * actually exercise - not by dragging an already-placed block past its neighbors.
 */
@Composable
private fun NoteBlocksList(
    blocks: List<EditableBlock>,
    focusTargetId: String?,
    onFocusTargetHandled: () -> Unit,
    onFocusChanged: (String) -> Unit,
    onBlockValueChange: (EditableBlock, TextFieldValue) -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
    onEnterPressed: (String) -> Unit,
    onBackspaceAtStart: (String) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onImageTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Keeps the focused block visible above the keyboard, the same "typing auto-scroll" backstop
    // this screen's own checklist editor already used: wait a couple of frames for the new layout
    // to settle, then scroll only the minimum necessary amount.
    LaunchedEffect(focusedBlockId, blocks) {
        val targetId = focusedBlockId ?: return@LaunchedEffect
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
        items(blocks, key = { it.id }) { block ->
            EditableBlockRow(
                block = block,
                requestFocus = focusTargetId == block.id,
                onFocusHandled = onFocusTargetHandled,
                onFocusChanged = { focused -> if (focused) { focusedBlockId = block.id; onFocusChanged(block.id) } },
                onValueChange = { newValue -> onBlockValueChange(block, newValue) },
                onCheckedChange = { checked -> onCheckedChange(block.id, checked) },
                onEnterPressed = { onEnterPressed(block.id) },
                onBackspaceAtStart = { onBackspaceAtStart(block.id) },
                onDeleteClick = { onDeleteBlock(block.id) },
                onImageTap = { onImageTap(block.id) }
            )
        }
    }
}

@Composable
private fun EditableBlockRow(
    block: EditableBlock,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onEnterPressed: () -> Unit,
    onBackspaceAtStart: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageTap: () -> Unit
) {
    when (block.type) {
        NoteBlockType.IMAGE -> {
            val imagePath = block.imagePath
            val bitmap = remember(imagePath) { imagePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onImageTap)
                )
            }
        }
        NoteBlockType.TEXT -> {
            val focusRequester = remember(block.id) { FocusRequester() }
            LaunchedEffect(requestFocus) {
                if (requestFocus) {
                    focusRequester.requestFocus()
                    onFocusHandled()
                }
            }
            val formatTransformation = remember(block.formatSpans) { noteFormatVisualTransformation(block.formatSpans) }
            TextField(
                value = block.value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.note_content_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                visualTransformation = formatTransformation,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = transparentTextFieldColors()
            )
        }
        NoteBlockType.BULLET, NoteBlockType.CHECKLIST -> {
            val focusRequester = remember(block.id) { FocusRequester() }
            LaunchedEffect(requestFocus) {
                if (requestFocus) {
                    focusRequester.requestFocus()
                    onFocusHandled()
                }
            }
            val formatTransformation = remember(block.formatSpans) { noteFormatVisualTransformation(block.formatSpans) }
            // Seeded from the block's own actual initial cursor position (not hardcoded true) -
            // a freshly-converted non-empty Bullet/Checklist item starts with its cursor at the
            // END of its text (see convertFocusedLinesToType), not the start, and this must
            // reflect that correctly even before the field's first onValueChange fires.
            var atLineStart by remember(block.id) {
                mutableStateOf(block.value.selection.collapsed && block.value.selection.start == 0)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (block.type == NoteBlockType.CHECKLIST) {
                    Box(modifier = Modifier.padding(top = 10.dp)) {
                        ChecklistCheckbox(checked = block.checked, onToggle = { onCheckedChange(!block.checked) })
                    }
                } else {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, end = 12.dp)
                    )
                }

                TextField(
                    value = block.value,
                    onValueChange = { newValue ->
                        atLineStart = newValue.selection.collapsed && newValue.selection.start == 0
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        // Best-effort backspace-at-start handling: Android soft keyboards don't
                        // always deliver a KeyEvent for a backspace that has nothing to delete
                        // forward of the cursor (an IME may instead call deleteSurroundingText
                        // directly, which never reaches onKeyEvent) - this reliably catches a
                        // hardware keyboard and many soft keyboards, but isn't guaranteed on
                        // every keyboard app/OS version. Enter (spec 3C/3D, 4C/4D) is the
                        // primary, reliable way to exit Bullet/Checklist mode - it doesn't
                        // depend on this at all.
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && atLineStart) {
                                onBackspaceAtStart()
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.checklist_item_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    visualTransformation = formatTransformation,
                    // singleLine is intentionally false so long text wraps onto further lines
                    // instead of scrolling off-screen; Enter is still never typed as a literal
                    // newline into the text because imeAction is Next below, not Default -
                    // Compose routes Enter to onNext instead of inserting "\n" whenever a
                    // non-default imeAction is set, even in multiline fields.
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = onEnterPressed),
                    colors = transparentTextFieldColors()
                )

                IconButton(onClick = onDeleteClick, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Spec's own "tap an image" interaction: Back (top-left) returns to the editor unchanged; Delete
 * (top-right) removes only that image block and returns to the editor - the note's title, other
 * blocks, and their order are all untouched either way. Rendered as a full-bleed overlay layered
 * on top of the editor's own Scaffold (see [NoteEditorScreen]'s own outer Box), rather than a
 * separate navigation route, since it's a transient focus state of the same editor session, not a
 * distinct screen. */
@Composable
private fun ImageFocusOverlay(imagePath: String, onBack: () -> Unit, onDelete: () -> Unit) {
    val bitmap = remember(imagePath) { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                    tint = Color.White
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.delete),
                    tint = Color.White
                )
            }
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
