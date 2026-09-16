package com.nj031.onetask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// The single calendar color palette used everywhere a date picker appears in One Task.
private val CalendarPrimaryBlue = Color(0xFF2F6FD6)
private val CalendarHighlightBlue = Color(0xFFDBEBFA)
private val CalendarCardWhite = Color(0xFFFFFFFF)
private val CalendarDarkText = Color(0xFF17365D)
private val CalendarSecondaryText = Color(0xFF6B7C93)

/**
 * The single calendar/date-picker presentation used everywhere in One Task (Homepage, Journal,
 * Add/Edit Task): a compact floating bottom panel. Tapping a day selects it and closes the
 * sheet immediately - there is no separate confirm step, matching the Homepage calendar this
 * was modeled on. [markedDates] draws a small dot under a day (e.g. Journal's "has notes"
 * indicator); [maxSelectableDate], when set, dims and disables any day after it (e.g. Journal
 * disallows picking a future date). [weekStartDay] only reorders which column each weekday
 * lands in (General Settings > Week Starts On) - it never changes a date's actual value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTaskCalendarSheet(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    markedDates: Set<LocalDate> = emptySet(),
    maxSelectableDate: LocalDate? = null,
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    CompactBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CalendarCardWhite
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarNavButton(
                    icon = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.previous_month),
                    onClick = { visibleMonth = visibleMonth.minusMonths(1) }
                )
                Text(
                    text = visibleMonth.format(calendarMonthYearFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CalendarPrimaryBlue
                )
                CalendarNavButton(
                    icon = Icons.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.next_month),
                    onClick = { visibleMonth = visibleMonth.plusMonths(1) }
                )
            }

            CalendarWeekdayHeader(weekStartDay = weekStartDay)

            CalendarMonthGrid(
                visibleMonth = visibleMonth,
                selectedDate = initialDate,
                markedDates = markedDates,
                maxSelectableDate = maxSelectableDate,
                weekStartDay = weekStartDay,
                onDayClick = { date ->
                    onDateSelected(date)
                    dismiss()
                }
            )

            TextButton(
                onClick = ::dismiss,
                contentPadding = PaddingValues(vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.close),
                    style = MaterialTheme.typography.bodySmall,
                    color = CalendarSecondaryText
                )
            }
        }
    }
}

/**
 * A deliberately smaller nav button than the stock [androidx.compose.material3.IconButton]
 * (whose own internal sizing always wins over a caller-supplied size modifier), so the
 * calendar's month header stays compact.
 */
@Composable
private fun CalendarNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = CalendarPrimaryBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CalendarWeekdayHeader(weekStartDay: DayOfWeek) {
    val labels = remember(weekStartDay) {
        orderedWeekDays(weekStartDay).map { day ->
            day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = CalendarSecondaryText
            )
        }
    }
}

/** The 7 days of the week in display order starting from [startDay] - e.g. starting from
 * WEDNESDAY yields [WED, THU, FRI, SAT, SUN, MON, TUE]. Used identically by the header labels
 * and the month grid's column layout so they always stay in sync. */
private fun orderedWeekDays(startDay: DayOfWeek): List<DayOfWeek> =
    (0..6).map { DayOfWeek.of((startDay.value - 1 + it) % 7 + 1) }

@Composable
private fun CalendarMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    maxSelectableDate: LocalDate?,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit
) {
    val daysInMonth = visibleMonth.lengthOfMonth()
    val firstDayOfMonth = visibleMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value - weekStartDay.value + 7) % 7
    val totalWeeks = (firstDayOffset + daysInMonth + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until totalWeeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val dayNumber = week * 7 + dayOfWeek - firstDayOffset + 1
                    val date = if (dayNumber in 1..daysInMonth) visibleMonth.atDay(dayNumber) else null
                    val isDisabled = date != null && maxSelectableDate != null && date.isAfter(maxSelectableDate)
                    CalendarDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isDisabled = isDisabled,
                        isMarked = date != null && markedDates.contains(date),
                        onClick = { date?.let(onDayClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isDisabled: Boolean,
    isMarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // Slightly wider than tall: the touch-target WIDTH (and the date number's own
            // size) stays exactly what it was, but each row takes noticeably less vertical
            // space - this is the main lever for the grid's overall height, since there's no
            // separate per-row gap to trim (cells already sit edge-to-edge).
            .aspectRatio(1.3f)
            .padding(1.dp)
            .then(if (date != null && !isDisabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(CalendarHighlightBlue, RoundedCornerShape(8.dp))
                                .border(1.dp, CalendarPrimaryBlue, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isDisabled -> CalendarSecondaryText.copy(alpha = 0.4f)
                            isSelected -> CalendarPrimaryBlue
                            else -> CalendarDarkText
                        }
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(4.dp)
                            .then(
                                if (isMarked) {
                                    Modifier.background(CalendarPrimaryBlue, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}

private val calendarMonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
