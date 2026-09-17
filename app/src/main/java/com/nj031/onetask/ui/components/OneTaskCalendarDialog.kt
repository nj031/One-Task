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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nj031.onetask.R
import com.nj031.onetask.ui.haptics.rememberHapticTick
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// The single calendar color palette used everywhere a date picker appears in One Task.
private val CalendarPrimaryBlue = Color(0xFF2F6FD6)
private val CalendarHighlightBlue = Color(0xFFDBEBFA)
private val CalendarCardWhite = Color(0xFFFFFFFF)
private val CalendarDarkText = Color(0xFF17365D)
private val CalendarSecondaryText = Color(0xFF6B7C93)

/**
 * The single calendar/date-picker presentation used everywhere in One Task (Homepage, Journal,
 * Profile's Date of Birth, Add/Edit Task): a centered rounded dialog with a month/year header,
 * prev/next navigation, weekday labels, a date grid, and Cancel/OK actions - matching Android's
 * standard date-picker structure, restyled with the One Task palette instead of a default
 * Material lavender theme. There is no separate large "selected date" display above the grid -
 * the selected day is shown only via its own highlighted cell inside the grid.
 *
 * Tapping a day only previews a selection inside the grid; nothing is applied via
 * [onDateSelected] until the user taps OK, and the preview is discarded entirely on Cancel or on
 * dismissing the dialog any other way (back press/tapping outside). [markedDates] draws a small
 * dot under a day (e.g. Journal's "has notes" indicator); [maxSelectableDate], when set, dims and
 * disables any day after it (e.g. Journal/Profile disallow picking a future date).
 * [weekStartDay] only reorders which column each weekday lands in (General Settings > Week
 * Starts On) - it never changes a date's actual value.
 */
@Composable
fun OneTaskCalendarDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    markedDates: Set<LocalDate> = emptySet(),
    maxSelectableDate: LocalDate? = null,
    weekStartDay: DayOfWeek = DayOfWeek.MONDAY
) {
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var pendingDate by remember { mutableStateOf(initialDate) }
    val hapticTick = rememberHapticTick()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CalendarCardWhite,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
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
                        style = MaterialTheme.typography.titleMedium,
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
                    selectedDate = pendingDate,
                    markedDates = markedDates,
                    maxSelectableDate = maxSelectableDate,
                    weekStartDay = weekStartDay,
                    onDayClick = { date ->
                        hapticTick()
                        pendingDate = date
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalendarSecondaryText
                        )
                    }
                    TextButton(
                        onClick = {
                            onDateSelected(pendingDate)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.date_picker_ok),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CalendarPrimaryBlue
                        )
                    }
                }
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
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = CalendarPrimaryBlue,
            modifier = Modifier.size(22.dp)
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
            .padding(top = 12.dp)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
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

/** A circular selected-date treatment (matching Android's standard date-picker convention)
 * instead of a filled rounded-square highlight - also consistent with the circular
 * checkbox/subtask indicators already used elsewhere in One Task (HomeScreen, FocusTimerScreen). */
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
            .aspectRatio(1f)
            .padding(2.dp)
            .then(if (date != null && !isDisabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(CalendarHighlightBlue, CircleShape)
                                .border(1.5.dp, CalendarPrimaryBlue, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
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
