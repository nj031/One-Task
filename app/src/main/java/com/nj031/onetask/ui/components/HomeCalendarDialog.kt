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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
 * The Tasks homepage's own calendar entry point: a centered dialog (not the compact bottom-sheet
 * [OneTaskCalendarSheet] Add Task still uses - this is deliberately a separate component so that
 * screen is completely unaffected). Every color comes from
 * [MaterialTheme.colorScheme] rather than a hardcoded palette, so this follows whichever theme
 * (light/dark) is currently active, including if it changes.
 *
 * Always opens on the current real-world month, regardless of what date is currently selected on
 * the Tasks homepage or what month was last browsed - [selectedDate] only controls which day (if
 * any, if it's in the visible month) shows the selected-date highlight, never where the calendar
 * opens. [markedDates] draws a small dot under any date that actually has a task, driven by
 * [onVisibleMonthChanged] telling the caller which month's tasks to look up as the user
 * navigates - the same reactive Flow-backed pattern already used for the rest of this screen's
 * date data, not a separate one-off query.
 *
 * [onDateSelected] fires (and the whole dialog closes) when a day in the grid is tapped, or when
 * Jump to Date's "Go to Date" is used - both go through the exact same callback the Tasks
 * homepage already used for its date navigation.
 */
@Composable
fun HomeCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    markedDates: Set<LocalDate> = emptySet(),
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
        // the Tasks page, matching the two-level "back closes one step at a time" behavior.
        JumpToDateDialog(
            initialDate = selectedDate,
            onDismiss = { showJumpToDate = false },
            onGoToDate = { date ->
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
                    HomeCalendarHeader(
                        visibleMonth = visibleMonth,
                        onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                        onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                        onJumpToDateClick = { showJumpToDate = true }
                    )

                    HomeCalendarWeekdayHeader(weekStartDay = weekStartDay)

                    HomeCalendarMonthGrid(
                        visibleMonth = visibleMonth,
                        selectedDate = selectedDate,
                        today = today,
                        markedDates = markedDates,
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

@Composable
private fun HomeCalendarHeader(
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
        HomeCalendarNavButton(
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

        HomeCalendarNavButton(
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
private fun HomeCalendarNavButton(
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
private fun HomeCalendarWeekdayHeader(weekStartDay: DayOfWeek) {
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

/** A day cell in [HomeCalendarMonthGrid] - either a real, selectable day in [visibleMonth], or a
 * muted, non-interactive filler number from the adjacent month (purely visual, matching the
 * reference calendar's leading/trailing days). */
private sealed class HomeCalendarCell {
    data class InMonth(val date: LocalDate) : HomeCalendarCell()
    data class Overflow(val dayNumber: Int) : HomeCalendarCell()
}

private fun buildCalendarCells(visibleMonth: YearMonth, weekStartDay: DayOfWeek): List<HomeCalendarCell> {
    val daysInMonth = visibleMonth.lengthOfMonth()
    val firstDayOfMonth = visibleMonth.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value - weekStartDay.value + 7) % 7
    val totalWeeks = (firstDayOffset + daysInMonth + 6) / 7
    val prevMonthLength = visibleMonth.minusMonths(1).lengthOfMonth()

    return (0 until totalWeeks * 7).map { index ->
        val dayNumber = index - firstDayOffset + 1
        when {
            dayNumber < 1 -> HomeCalendarCell.Overflow(prevMonthLength + dayNumber)
            dayNumber > daysInMonth -> HomeCalendarCell.Overflow(dayNumber - daysInMonth)
            else -> HomeCalendarCell.InMonth(visibleMonth.atDay(dayNumber))
        }
    }
}

@Composable
private fun HomeCalendarMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    markedDates: Set<LocalDate>,
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
                    HomeCalendarDayCell(
                        cell = cell,
                        isSelected = cell is HomeCalendarCell.InMonth && cell.date == selectedDate,
                        isToday = cell is HomeCalendarCell.InMonth && cell.date == today,
                        isMarked = cell is HomeCalendarCell.InMonth && markedDates.contains(cell.date),
                        onClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCalendarDayCell(
    cell: HomeCalendarCell,
    isSelected: Boolean,
    isToday: Boolean,
    isMarked: Boolean,
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
            is HomeCalendarCell.Overflow -> {
                Text(
                    text = cell.dayNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            is HomeCalendarCell.InMonth -> {
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
                        .clickable { onClick(cell.date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = cell.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
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

/**
 * The Jump to Date experience: independently scrollable Month/Day/Year columns, each keeping its
 * selected value centered and highlighted, then a Go to Date button. There is no separate date
 * readout field above the columns (deliberately removed per spec). Changing month or year clamps
 * the picked day down when it would otherwise land past the end of the new month, so Go to Date
 * can never build an invalid date (e.g. Feb 30) - the day column's own range always reflects
 * exactly the valid days for whatever month/year is currently picked.
 */
@Composable
private fun JumpToDateDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onGoToDate: (LocalDate) -> Unit
) {
    var pickedYear by remember { mutableStateOf(initialDate.year) }
    var pickedMonth by remember { mutableStateOf(initialDate.monthValue) }
    var pickedDay by remember { mutableStateOf(initialDate.dayOfMonth) }

    val daysInPickedMonth = remember(pickedYear, pickedMonth) {
        YearMonth.of(pickedYear, pickedMonth).lengthOfMonth()
    }
    if (pickedDay > daysInPickedMonth) {
        pickedDay = daysInPickedMonth
    }

    val currentYear = remember { LocalDate.now().year }
    val monthLabels = remember { (1..12).map(::monthShortName) }
    val dayLabels = remember(daysInPickedMonth) { (1..daysInPickedMonth).map(Int::toString) }
    val yearLabels = remember(currentYear) {
        (currentYear - JUMP_TO_DATE_YEAR_SPAN..currentYear + JUMP_TO_DATE_YEAR_SPAN).map(Int::toString)
    }
    val minYear = currentYear - JUMP_TO_DATE_YEAR_SPAN

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.jump_to_date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    DateWheelColumn(
                        items = monthLabels,
                        selectedIndex = pickedMonth - 1,
                        onSelectedIndexChange = { pickedMonth = it + 1 },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = dayLabels,
                        selectedIndex = pickedDay - 1,
                        onSelectedIndexChange = { pickedDay = it + 1 },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = yearLabels,
                        selectedIndex = pickedYear - minYear,
                        onSelectedIndexChange = { pickedYear = minYear + it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            onGoToDate(LocalDate.of(pickedYear, pickedMonth, pickedDay))
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.go_to_date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private const val JUMP_TO_DATE_YEAR_SPAN = 100
