package com.nj031.onetask.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.journal.ChecklistItem
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.viewmodel.JournalViewModel
import kotlinx.coroutines.isActive

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
                // Consuming it here shrinks this Box's visible height as the keyboard animates
                // in, which is what lets either the TEXT field's own scroll container or the
                // CHECKLIST's LazyColumn actually scroll far enough to keep the active line/item
                // above the keyboard.
                .imePadding(),
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

/**
 * The checklist content area: a reorderable (long-press and drag), auto-scrolling list of items,
 * each a circular checkbox plus its (wrapping, multiline-capable) text, and a trailing "Add item"
 * row. Pressing Enter on an item creates a new one directly below it and focuses it; Enter on an
 * already-empty item is a no-op, so it can never be used to pile up blank rows.
 */
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
    // translation), and displayedItems is the on-screen order - normally just [items], but during
    // a drag it's the optimistic, already-swapped order so rows visibly shift before the reorder
    // is persisted back up to the note.
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var displayedItems by remember { mutableStateOf(items) }

    LaunchedEffect(items, draggedItemId) {
        if (draggedItemId == null) {
            displayedItems = items
        }
    }

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
    LaunchedEffect(focusedItemId, displayedItems) {
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
        items(displayedItems, key = { it.id }) { item ->
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
                    val (reordered, correctedOffset) = checklistDragSwapIfNeeded(
                        items = displayedItems,
                        draggedItemId = item.id,
                        dragOffsetY = dragOffsetY,
                        listState = listState
                    )
                    displayedItems = reordered
                    dragOffsetY = correctedOffset
                },
                onDragEnd = {
                    val finalOrder = displayedItems
                    draggedItemId = null
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
                // A drag-to-reorder gesture that only activates after Android's own standard
                // long-press timeout - not a custom one - so a normal short tap into the text
                // field (to place the cursor, or type) keeps working exactly as before, and is
                // never mistaken for the start of a drag.
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
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.padding(top = 10.dp)) {
                ChecklistCheckbox(checked = item.checked, onToggle = { onCheckedChange(!item.checked) })
            }

            TextField(
                value = item.text,
                onValueChange = onTextChange,
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
