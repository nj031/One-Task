package com.nj031.onetask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * The wheel-column primitives shared by every scrollable Month/Day/Year date picker in One Task
 * (the Tasks homepage's Jump to Date, and Profile's Date of Birth picker) - kept in one place so
 * both reuse the exact same scroll/snap/centered-highlight mechanism rather than each having its
 * own copy of this logic.
 */
internal val DateWheelRowHeight = 44.dp
internal const val DateWheelVisibleRows = 3

@Composable
internal fun DateWheelColumnDivider() {
    // An explicit height matching the wheel columns' own fixed height, not fillMaxHeight(): a
    // divider inside a Row whose parent Column doesn't bound height would otherwise resolve
    // fillMaxHeight() against the enclosing Dialog's full available height instead of the wheel
    // columns' actual size, stretching the whole dialog to near-screen height.
    Box(
        modifier = Modifier
            .height(DateWheelRowHeight * DateWheelVisibleRows)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

/**
 * One independently-scrollable wheel: [items] rendered as a vertical list, [selectedIndex]
 * always kept in the visually centered row (a fixed highlighted band drawn behind the list, not
 * per-item styling, so it never jumps between items while scrolling). Dragging and letting go
 * settles on whichever item ends up nearest that center via [androidx.compose.foundation.lazy.LazyListState]'s own
 * real, per-item layout info - the same "find what's nearest a target position" technique the
 * drag-and-drop task reordering already uses - then reports that item's index back through
 * [onSelectedIndexChange]. Tapping any row scrolls straight to it as a shortcut.
 */
@Composable
internal fun DateWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()

    // Keeps the wheel in sync whenever selectedIndex changes for a reason other than the user's
    // own scroll (e.g. the day column's range shrinking when the month changes) - a no-op while
    // the user is actively dragging this exact wheel.
    LaunchedEffect(selectedIndex, items) {
        if (!listState.isScrollInProgress) {
            listState.scrollToItem(selectedIndex)
        }
    }

    // Once a drag/fling settles, snap to whichever item ended up closest to the column's center
    // and report it as the new selection.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val viewportCenter =
                (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            val centered = listState.layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }
            if (centered != null) {
                if (centered.index != selectedIndex) {
                    onSelectedIndexChange(centered.index)
                }
                listState.animateScrollToItem(centered.index)
            }
        }
    }

    Box(modifier = modifier.height(DateWheelRowHeight * DateWheelVisibleRows)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(DateWheelRowHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = DateWheelRowHeight)
        ) {
            itemsIndexed(items) { index, label ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DateWheelRowHeight)
                        .clickable {
                            onSelectedIndexChange(index)
                            scope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = if (isSelected) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

internal fun monthShortName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
