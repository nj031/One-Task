package com.nj031.onetask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nj031.onetask.R
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskCalendarIcon
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * THE single canonical Calendar for the whole One Task app - the source of truth for "pick a
 * date from a month grid" everywhere it's needed (the Tasks homepage, Add Task's own Date field,
 * Add Task's Reminder custom date, and any future feature with the same need). Do not copy this
 * file's contents into a new screen; call this composable instead, or extend it with a new
 * parameter if a genuine new requirement arises.
 *
 * This is a different component from [OneTaskDatePickerDialog]: this is a month grid you tap a
 * day in, that one is three independently-scrollable Month/Day/Year wheels - see that file's own
 * doc comment for why they stay separate.
 *
 * Always opens on the current real-world month, regardless of what date is currently selected or
 * what month was last browsed - [selectedDate] only controls which day (if any, if it's in the
 * visible month) shows the selected-date highlight, never where the calendar opens.
 * [markedDates] draws a small dot under any date that actually has a task/note, driven by
 * [onVisibleMonthChanged] telling the caller which month's data to look up as the user navigates.
 * [minSelectableDate]/[maxSelectableDate], when set, dim and disable any day outside that range
 * (e.g. a Reminder's custom date must never be in the past) without restricting month navigation
 * itself - the user can still browse to see what's there, just not select an out-of-range day.
 *
 * [onDateSelected] fires (and the whole dialog closes) when a day in the grid is tapped, or when
 * Jump to Date's "Go to Date" is used - both go through the exact same callback.
 */
@Composable
fun OneTaskCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    markedDates: Set<LocalDate> = emptySet(),
    minSelectableDate: LocalDate? = null,
    maxSelectableDate: LocalDate? = null,
    onVisibleMonthChanged: (YearMonth) -> Unit = {},
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY
) {
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var showJumpToDate by remember { mutableStateOf(false) }
    val hapticTick = rememberHapticTick()

    LaunchedEffect(visibleMonth) { onVisibleMonthChanged(visibleMonth) }

    if (showJumpToDate) {
        // A separate Dialog layered in place of the calendar (not on top of it) - its own
        // back-press/outside-tap only returns here to the month grid, never all the way out to
        // the caller, matching the two-level "back closes one step at a time" behavior. The same
        // min/max bound the grid enforces is passed through here too, so Jump to Date can never
        // be used to bypass it - falling back to a wide +/-100 year span when unset, matching
        // this dialog's own previously-unrestricted Jump to Date behavior.
        OneTaskDatePickerDialog(
            initialDate = selectedDate,
            minDate = minSelectableDate ?: today.minusYears(JUMP_TO_DATE_YEAR_SPAN.toLong()),
            maxDate = maxSelectableDate ?: today.plusYears(JUMP_TO_DATE_YEAR_SPAN.toLong()),
            onDismiss = { showJumpToDate = false },
            onDateSelected = { date ->
                showJumpToDate = false
                onDateSelected(date)
                onDismiss()
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    CalendarDialogHeader(
                        visibleMonth = visibleMonth,
                        onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                        onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                        onJumpToDateClick = { showJumpToDate = true }
                    )

                    CalendarDialogWeekdayHeader(weekStartDay = weekStartDay)

                    CalendarDialogMonthGrid(
                        visibleMonth = visibleMonth,
                        selectedDate = selectedDate,
                        today = today,
                        markedDates = markedDates,
                        minSelectableDate = minSelectableDate,
                        maxSelectableDate = maxSelectableDate,
                        weekStartDay = weekStartDay,
                        onDayClick = { date ->
                            hapticTick()
                            onDateSelected(date)
                            onDismiss()
                        }
                    )

                    TextButton(
                        onClick = {
                            hapticTick()
                            visibleMonth = YearMonth.from(today)
                            onDateSelected(today)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.jump_to_today),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private const val JUMP_TO_DATE_YEAR_SPAN = 100

@Composable
private fun CalendarDialogHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToDateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarDialogNavButton(
            icon = Icons.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(id = R.string.previous_month),
            onClick = onPreviousMonth
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onJumpToDateClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visibleMonth.format(calendarMonthYearFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.jump_to_date),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(18.dp)
            )
        }

        CalendarDialogNavButton(
            icon = Icons.Filled.KeyboardArrowRight,
            contentDescription = stringResource(id = R.string.next_month),
            onClick = onNextMonth
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onJumpToDateClick),
            contentAlignment = Alignment.Center
        ) {
            OneTaskCalendarIcon(size = 18.dp)
        }
    }
}

@Composable
private fun CalendarDialogNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CalendarDialogWeekdayHeader(weekStartDay: DayOfWeek) {
    val labels = remember(weekStartDay) {
        orderedWeekDays(weekStartDay).map { day ->
            day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The 7 days of the week in display order starting from [startDay] - e.g. starting from
 * WEDNESDAY yields [WED, THU, FRI, SAT, SUN, MON, TUE]. */
private fun orderedWeekDays(startDay: DayOfWeek): List<DayOfWeek> =
    (0..6).map { DayOfWeek.of((startDay.value - 1 + it) % 7 + 1) }

/** A day cell in [CalendarDialogMonthGrid] - either a real, selectable day in [visibleMonth], or
 * a muted, non-interactive filler number from the adjacent month (purely visual). */
private sealed class CalendarDialogCell {
    data class InMonth(val date: LocalDate) : CalendarDialogCell()
    data class Overflow(val dayNumber: Int) : CalendarDialogCell()
}

private fun buildCalendarCells(visibleMonth: YearMonth, weekStartDay: DayOfWeek): List<CalendarDialogCell> {
    val daysInMonth = visibleMonth.lengthOfMonth()
    val firstDayOfMonth = visibleMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value - weekStartDay.value + 7) % 7
    val totalWeeks = (firstDayOffset + daysInMonth + 6) / 7
    val prevMonthLength = visibleMonth.minusMonths(1).lengthOfMonth()

    return (0 until totalWeeks * 7).map { index ->
        val dayNumber = index - firstDayOffset + 1
        when {
            dayNumber < 1 -> CalendarDialogCell.Overflow(prevMonthLength + dayNumber)
            dayNumber > daysInMonth -> CalendarDialogCell.Overflow(dayNumber - daysInMonth)
            else -> CalendarDialogCell.InMonth(visibleMonth.atDay(dayNumber))
        }
    }
}

@Composable
private fun CalendarDialogMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    markedDates: Set<LocalDate>,
    minSelectableDate: LocalDate?,
    maxSelectableDate: LocalDate?,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit
) {
    val cells = remember(visibleMonth, weekStartDay) { buildCalendarCells(visibleMonth, weekStartDay) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val isDisabled = cell is CalendarDialogCell.InMonth && (
                        (minSelectableDate != null && cell.date.isBefore(minSelectableDate)) ||
                            (maxSelectableDate != null && cell.date.isAfter(maxSelectableDate))
                        )
                    CalendarDialogDayCell(
                        cell = cell,
                        isSelected = cell is CalendarDialogCell.InMonth && cell.date == selectedDate,
                        isToday = cell is CalendarDialogCell.InMonth && cell.date == today,
                        isMarked = cell is CalendarDialogCell.InMonth && markedDates.contains(cell.date),
                        isDisabled = isDisabled,
                        onClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDialogDayCell(
    cell: CalendarDialogCell,
    isSelected: Boolean,
    isToday: Boolean,
    isMarked: Boolean,
    isDisabled: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        when (cell) {
            is CalendarDialogCell.Overflow -> {
                Text(
                    text = cell.dayNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            is CalendarDialogCell.InMonth -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .then(
                            when {
                                isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                // Today's own highlight is deliberately lighter than the selected
                                // fill - a tinted background plus a thin outline - so it never
                                // reads as "selected" when it isn't.
                                isToday -> Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else -> Modifier
                            }
                        )
                        .then(if (!isDisabled) Modifier.clickable { onClick(cell.date) } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = cell.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(4.dp)
                                .then(
                                    if (isMarked) {
                                        val dotColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Modifier.background(dotColor, CircleShape)
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
}

private val calendarMonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
