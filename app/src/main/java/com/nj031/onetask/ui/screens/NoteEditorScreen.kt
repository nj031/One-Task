package com.nj031.onetask.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
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
import kotlinx.coroutines.launch

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
    // which is captured once at composition entry and never updates: Note Info must react the
    // moment a new note becomes real, without needing the editor to be closed and reopened
    // (which is what used to create a fresh, non-null existingNote instead) - and commitOnExit
    // needs it to know whether to create or update.
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

    // Removes a block outright (never leaving zero blocks - a lone empty TEXT block takes its
    // place if it was the only one) and moves focus to whatever is now the previous block, for the
    // two "this edit emptied the block out" paths below - backspace at an already-empty block's
    // start, and a single edit (backspace, select-all-delete, etc.) that empties a BULLET/
    // CHECKLIST block's content in one go. Distinct from deleteBlock, which is the explicit "x"
    // tap on a checklist item and deliberately does not move focus (the keyboard may not even be
    // open when that's tapped).
    fun removeBlockAndRetarget(blockId: String) {
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val remaining = blocks.toMutableList().apply { removeAt(index) }
        blocks = remaining.ifEmpty { listOf(EditableBlock(type = NoteBlockType.TEXT)) }
        focusTargetId = remaining.getOrNull((index - 1).coerceAtLeast(0))?.id
    }

    // The checklist item "x" control (see EditableBlockRow) - removes only that item/block, never
    // the whole note, never a neighboring item, and never leaves an empty item behind, since the
    // block itself (checked state included) is simply dropped from the list.
    fun deleteBlock(blockId: String) {
        blocks = blocks.filterNot { it.id == blockId }.ifEmpty { listOf(EditableBlock(type = NoteBlockType.TEXT)) }
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
                    // Pin/Unpin now lives only in the Notes list's own action menu (see
                    // NotesScreen's NoteActionSheet), which writes it immediately, independent of
                    // this deferred save - re-asserting the note's own current isPinned here
                    // (rather than some other value) just carries that state through unchanged,
                    // since this screen has no way to change it itself.
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
        // A single edit (holding backspace, select-all + delete, etc.) that empties a BULLET/
        // CHECKLIST item's content out completely: the item/block disappears immediately rather
        // than lingering as an empty row waiting for one more backspace press at its (now) start -
        // a list item is a semantic block here, not just a visual marker, so emptying its content
        // removes the block itself. Guarded on block.value.text.isNotEmpty() so this never fires
        // for a brand-new, already-empty item's very first keystroke.
        if ((block.type == NoteBlockType.BULLET || block.type == NoteBlockType.CHECKLIST) &&
            newValue.text.isEmpty() && block.value.text.isNotEmpty()
        ) {
            removeBlockAndRetarget(block.id)
            return
        }
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
            // Converting this block's type changes which branch of EditableBlockRow's own
            // when(block.type) composes for it (BULLET/CHECKLIST -> TEXT), which are structurally
            // different composables even though the LazyColumn item key (blockId) is unchanged -
            // Compose tears down the old (focused) BasicTextField and composes a brand new,
            // unfocused one in its place. Without explicitly re-requesting focus here, that new
            // TEXT field never claims it, the IME loses its input target, and the keyboard hides -
            // this is the actual, confirmed cause of "keyboard hides after exiting an empty list
            // item," not something a global "keep keyboard open" flag would fix. Retargeting focus
            // at the same id (exactly like the non-empty branch below and removeBlockAndRetarget
            // already do for their own newly-relevant blocks) lets EditableBlockRow's existing
            // LaunchedEffect(requestFocus) reclaim it the instant the new TEXT field composes.
            focusTargetId = blockId
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
        val block = blocks.find { it.id == blockId } ?: return
        if (block.value.text.isEmpty()) {
            removeBlockAndRetarget(blockId)
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
        } else if (block.type == NoteBlockType.TEXT) {
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
        // Guarantees there is always a valid editable text position after an image that ends up
        // at the end of the document (spec requirement) - none of the three branches above add a
        // trailing TEXT block when there's no leftover text to preserve (e.g. the cursor was at
        // the very end, or blocks was empty, or the target block wasn't TEXT), which previously
        // could leave an IMAGE as the note's last block with nothing to tap/type into below it.
        blocks = blocks.ensureEditableTrailingPosition()
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

    // Two mutually-exclusive handlers (never both enabled at once) rather than one relying on
    // registration-order priority: while the image-focused overlay is open, back must only
    // dismiss IT (returning to this same editor, note unchanged) - previously there was no
    // handler for that state at all, so a system back press fell through to this one and closed
    // the whole editor instead.
    BackHandler(enabled = focusedImageBlockId == null) {
        commitOnExit()
        onDone()
    }
    BackHandler(enabled = focusedImageBlockId != null) {
        focusedImageBlockId = null
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
                        onShareClick = ::performShare,
                        onAddToLabelClick = { showLabelDialog = true },
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
                        onDeleteBlock = ::deleteBlock,
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
    onShareClick: () -> Unit,
    onAddToLabelClick: () -> Unit,
    onNoteInfoClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
}.ensureEditableTrailingPosition()

/** Appends an empty TEXT block whenever [this] ends in an IMAGE block, so there is always a
 * valid editable text position after an image that's at the end of the document - applied both
 * when a note is first loaded (fixing a note saved before this guarantee existed) and every time
 * [NoteEditorScreen]'s own insertImageBlock adds a new image. */
private fun List<EditableBlock>.ensureEditableTrailingPosition(): List<EditableBlock> =
    if (lastOrNull()?.type == NoteBlockType.IMAGE) this + EditableBlock(type = NoteBlockType.TEXT) else this

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
    val listState = rememberLazyListState()

    // A newly-created block (from Enter, or from converting a line to Bullet/Checklist) is
    // inserted right after the block the user was just editing. Unlike a plain Column, LazyColumn
    // only composes items that are within (or immediately next to) its current viewport - so if
    // that previous block was already at/near the viewport's bottom edge (exactly the "current
    // item visible just above the keyboard" situation this is meant to fix), the brand-new item
    // can end up not composed at all yet. EditableBlockRow's own LaunchedEffect(requestFocus) then
    // has nothing to attach focus to, and BringIntoViewRequester.bringIntoView() - which only runs
    // once a field is actually focused - never fires. This is the confirmed mechanism (from
    // LazyColumn's own lazy composition model) behind a newly-created item appearing to vanish
    // below the keyboard. Nudging the list to bring the new item's index into the composed range
    // first lets the existing focus + bringIntoView chain in EditableBlockRow take over for the
    // final, precise positioning - this only runs when the previous block was actually near the
    // bottom edge, so an Enter pressed in the middle of a long, already-scrolled note (where the
    // next line is already comfortably on-screen) never triggers an unrelated jump.
    LaunchedEffect(focusTargetId) {
        val targetId = focusTargetId ?: return@LaunchedEffect
        val targetIndex = blocks.indexOfFirst { it.id == targetId }
        if (targetIndex <= 0) return@LaunchedEffect
        val previousBlockId = blocks[targetIndex - 1].id
        val info = listState.layoutInfo
        val previousItem = info.visibleItemsInfo.find { it.key == previousBlockId }
        val previousNearBottomEdge = previousItem == null || (previousItem.offset + previousItem.size) >= info.viewportEndOffset
        if (previousNearBottomEdge) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    // "Write your notes..." (see EditableBlockRow's TEXT branch) is only appropriate for the
    // genuine "whole note is blank" starting state - a single empty TEXT block. Any other empty
    // TEXT block (the line left behind by exiting an empty Bullet/Checklist item, or the trailing
    // line below an image) exists alongside other real content, so it isn't "this note is empty,
    // write something" - it's just an ordinary blank line.
    val isOnlyBlock = blocks.size == 1
    LazyColumn(state = listState, modifier = modifier) {
        items(blocks, key = { it.id }) { block ->
            EditableBlockRow(
                block = block,
                requestFocus = focusTargetId == block.id,
                onFocusHandled = onFocusTargetHandled,
                onFocusChanged = { focused -> if (focused) onFocusChanged(block.id) },
                onValueChange = { newValue -> onBlockValueChange(block, newValue) },
                onCheckedChange = { checked -> onCheckedChange(block.id, checked) },
                onEnterPressed = { onEnterPressed(block.id) },
                onBackspaceAtStart = { onBackspaceAtStart(block.id) },
                onDeleteClick = { onDeleteBlock(block.id) },
                onImageTap = { onImageTap(block.id) },
                showEmptyTextPlaceholder = isOnlyBlock
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    onImageTap: () -> Unit,
    showEmptyTextPlaceholder: Boolean
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
            val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()
            var isFocused by remember(block.id) { mutableStateOf(false) }
            var textLayoutResult by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }
            LaunchedEffect(requestFocus) {
                if (requestFocus) {
                    focusRequester.requestFocus()
                    onFocusHandled()
                }
            }
            // Requests only the cursor's own line into view, not the field's entire bounding box
            // (bringIntoViewRequester.bringIntoView() with no rect - what every call site here
            // used before - asks to show the WHOLE component). That's the confirmed root cause of
            // auto-scroll silently stopping partway through continuous typing: once a field's own
            // height (it's an unbounded-height, wrap-content BasicTextField - text simply keeps
            // growing as more lines are typed/wrapped) exceeds the visible area above the
            // keyboard/toolbar, "bring the whole field into view" can no longer be satisfied at
            // all, so it settles on showing the TOP of the field instead of the cursor's current
            // (much lower) line - which reads as "it worked initially, then stopped," with the
            // cursor stuck below the keyboard from then on. Asking for just the cursor's own rect
            // has no such ceiling: it stays satisfiable no matter how tall the field grows.
            fun bringCursorIntoView() {
                val layout = textLayoutResult ?: return
                val cursorOffset = block.value.selection.end.coerceIn(0, layout.layoutInput.text.length)
                coroutineScope.launch { bringIntoViewRequester.bringIntoView(layout.getCursorRect(cursorOffset)) }
            }
            val formatTransformation = remember(block.formatSpans) { noteFormatVisualTransformation(block.formatSpans) }
            BasicTextField(
                value = block.value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .focusRequester(focusRequester)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                        onFocusChanged(it.isFocused)
                        if (it.isFocused) bringCursorIntoView()
                    }
                    // Re-validates visibility whenever this field's own layout position/size
                    // changes while it's focused - not just once, at the moment focus is gained.
                    // A single bringIntoView() call on focus gain can run before the IME has
                    // actually finished its (asynchronous) show/hide animation, computing against
                    // a viewport that hasn't settled to its final size yet; as the surrounding
                    // Scaffold/LazyColumn keep reflowing while the keyboard animates, this field's
                    // global position keeps changing too, and each change re-checks the cursor
                    // against the CURRENT layout. A no-op once the cursor is already visible, so
                    // this never causes a jump while the user is just typing normally.
                    .onGloballyPositioned {
                        if (isFocused) bringCursorIntoView()
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                visualTransformation = formatTransformation,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                onTextLayout = { layout ->
                    textLayoutResult = layout
                    // The trigger that actually matters for "keep scrolling as I keep typing": a
                    // new line (from wrapping or a literal Enter) changes the text layout, which
                    // is exactly when the cursor's own rect moves to a new position that may now
                    // be outside the visible area.
                    if (isFocused) bringCursorIntoView()
                },
                decorationBox = { innerTextField ->
                    Box {
                        if (block.value.text.isEmpty() && showEmptyTextPlaceholder) {
                            Text(
                                text = stringResource(id = R.string.note_content_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        NoteBlockType.BULLET, NoteBlockType.CHECKLIST -> {
            val focusRequester = remember(block.id) { FocusRequester() }
            val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()
            var isFocused by remember(block.id) { mutableStateOf(false) }
            var textLayoutResult by remember(block.id) { mutableStateOf<TextLayoutResult?>(null) }
            LaunchedEffect(requestFocus) {
                if (requestFocus) {
                    focusRequester.requestFocus()
                    onFocusHandled()
                }
            }
            // See the TEXT branch's own comment on this same helper - identical shared-cause fix
            // for the auto-scroll cutoff, applied here too since a long, wrapped bullet/checklist
            // item can just as easily grow taller than the visible viewport.
            fun bringCursorIntoView() {
                val layout = textLayoutResult ?: return
                val cursorOffset = block.value.selection.end.coerceIn(0, layout.layoutInput.text.length)
                coroutineScope.launch { bringIntoViewRequester.bringIntoView(layout.getCursorRect(cursorOffset)) }
            }
            val formatTransformation = remember(block.formatSpans) { noteFormatVisualTransformation(block.formatSpans) }
            // Seeded from the block's own actual initial cursor position (not hardcoded true) -
            // a freshly-converted non-empty Bullet/Checklist item starts with its cursor at the
            // END of its text (see convertFocusedLinesToType), not the start, and this must
            // reflect that correctly even before the field's first onValueChange fires.
            var atLineStart by remember(block.id) {
                mutableStateOf(block.value.selection.collapsed && block.value.selection.start == 0)
            }

            // verticalAlignment = CenterVertically (not Top) so the bullet glyph/checkbox sit on
            // the SAME line as the first line of text - BasicTextField (unlike the Material3
            // TextField this used to be) has no built-in minimum height or internal content
            // padding, so there's no leftover chrome pushing the marker and the text apart or
            // inflating each row's height.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (block.type == NoteBlockType.CHECKLIST) {
                    ChecklistCheckbox(checked = block.checked, onToggle = { onCheckedChange(!block.checked) })
                } else {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }

                BasicTextField(
                    value = block.value,
                    onValueChange = { newValue ->
                        atLineStart = newValue.selection.collapsed && newValue.selection.start == 0
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            onFocusChanged(it.isFocused)
                            if (it.isFocused) bringCursorIntoView()
                        }
                        // Re-validates visibility whenever this row's own layout position/size
                        // changes while focused (IME animation settling, or text wrapping to a
                        // new line) rather than only once at focus-gain time - see the TEXT
                        // branch's own onGloballyPositioned comment above for why.
                        .onGloballyPositioned {
                            if (isFocused) bringCursorIntoView()
                        }
                        // Backspace-at-start handling for an empty Bullet/Checklist item. Uses
                        // onPreviewKeyEvent (capture phase, fires on the way DOWN to the focused
                        // field) rather than onKeyEvent (bubble phase, fires on the way back UP) -
                        // BasicTextField's own internal key handling sits between the two and can
                        // otherwise consume a backspace key event for its own (here, no-op, since
                        // the field is already empty) edit handling before onKeyEvent ever sees it,
                        // which was the actual, confirmed cause of backspace-at-an-empty-item
                        // sometimes doing nothing at all. Intercepting in the preview/capture phase
                        // guarantees this modifier sees the key first, regardless of what the field
                        // itself would otherwise have done with it; every other key still falls
                        // through untouched (returns false) to the field exactly as before.
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && atLineStart) {
                                onBackspaceAtStart()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    visualTransformation = formatTransformation,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    // singleLine is intentionally false so long text wraps onto further lines
                    // instead of scrolling off-screen; Enter is still never typed as a literal
                    // newline into the text because imeAction is Next below, not Default -
                    // Compose routes Enter to onNext instead of inserting "\n" whenever a
                    // non-default imeAction is set, even in multiline fields.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { onEnterPressed() }),
                    onTextLayout = { layout ->
                        textLayoutResult = layout
                        if (isFocused) bringCursorIntoView()
                    },
                    decorationBox = { innerTextField ->
                        Box {
                            if (block.value.text.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.checklist_item_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Checklist items get an explicit delete control (bullets deliberately do not -
                // bullets are removed via backspace only, per spec); tapping it removes only this
                // item/block, never the whole note or a neighboring item, and its checked state
                // disappears along with it since the entire block is dropped from the list.
                if (block.type == NoteBlockType.CHECKLIST) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
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
    // Top bar is its own opaque region above the image (not overlaid on top of it): previously
    // Back/Delete sat directly on top of the full-bleed image with no background of their own, so
    // a light/white area of the photo right under the icons could make a white icon unreadable.
    // Placing them on the screen's normal themed background instead - the same one every other
    // top bar in this app uses - keeps their contrast independent of whatever the photo contains,
    // and correct in both light and dark mode automatically.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.delete),
                    // The app's existing destructive/error color role (same one Notes list's own
                    // Delete uses) rather than a new color introduced just for this button - it's
                    // defined for readable contrast against the theme background in both light
                    // and dark mode.
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
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
