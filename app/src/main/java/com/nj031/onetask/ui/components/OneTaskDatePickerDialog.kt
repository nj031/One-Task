package com.nj031.onetask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nj031.onetask.R
import java.time.LocalDate
import java.time.YearMonth

/**
 * THE single canonical Month/Day/Year wheel Date Picker for the whole One Task app - the source
 * of truth for "pick an exact date via scrolling wheels" everywhere it's needed (the Tasks
 * homepage's Jump to Date via [OneTaskCalendarDialog], Edit Profile's Date of Birth, and any
 * future feature with the same need). Do not copy this file's contents into a new screen; call
 * this composable instead, or extend it with a new parameter if a genuine new requirement arises.
 *
 * This is deliberately a different component from [OneTaskCalendarDialog]: a Calendar is a month
 * grid you tap a day in, this is three independently-scrollable wheels - see each file's own doc
 * comment for why they stay separate rather than being merged into one generic component.
 *
 * [minDate]/[maxDate] bound every wheel: the year wheel only ever shows [minDate].year..
 * [maxDate].year, and the month/day wheels shrink further at the two boundary years so an
 * out-of-range date is never constructible (Edit Profile's Date of Birth relies on this to
 * structurally exclude every future date - see its call site). Pass a wide span (e.g. today
 * +/-100 years) for an effectively-unrestricted picker like Jump to Date.
 *
 * [initialDate] is clamped into [minDate]..[maxDate] before being used to seed the wheels, so a
 * caller never has to pre-clamp it themselves.
 */
@Composable
fun OneTaskDatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    title: String = stringResource(id = R.string.jump_to_date),
    confirmButtonText: String = stringResource(id = R.string.go_to_date)
) {
    val clampedInitial = remember(initialDate, minDate, maxDate) {
        clampDateToBounds(initialDate, minDate, maxDate)
    }

    var pickedYear by remember { mutableStateOf(clampedInitial.year) }
    var pickedMonth by remember { mutableStateOf(clampedInitial.monthValue) }
    var pickedDay by remember { mutableStateOf(clampedInitial.dayOfMonth) }

    val yearRange = yearWheelRange(minDate, maxDate)
    if (pickedYear !in yearRange) {
        pickedYear = pickedYear.coerceIn(yearRange)
    }

    val monthRange = monthWheelRange(pickedYear, minDate, maxDate)
    if (pickedMonth !in monthRange) {
        pickedMonth = pickedMonth.coerceIn(monthRange)
    }

    val dayRange = dayWheelRange(pickedYear, pickedMonth, minDate, maxDate)
    if (pickedDay !in dayRange) {
        pickedDay = pickedDay.coerceIn(dayRange)
    }

    val monthLabels = remember(monthRange) { monthRange.map(::monthShortName) }
    val dayLabels = remember(dayRange) { dayRange.map(Int::toString) }
    val yearLabels = remember(yearRange) { yearRange.map(Int::toString) }

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
                        text = title,
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
                        selectedIndex = pickedMonth - monthRange.first,
                        onSelectedIndexChange = { pickedMonth = monthRange.first + it },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = dayLabels,
                        selectedIndex = pickedDay - dayRange.first,
                        onSelectedIndexChange = { pickedDay = dayRange.first + it },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = yearLabels,
                        selectedIndex = pickedYear - yearRange.first,
                        onSelectedIndexChange = { pickedYear = yearRange.first + it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onDateSelected(LocalDate.of(pickedYear, pickedMonth, pickedDay)) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmButtonText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/** The full range of selectable years given [minDate]/[maxDate] - always non-empty since
 * [minDate] must not be after [maxDate]. */
internal fun yearWheelRange(minDate: LocalDate, maxDate: LocalDate): IntRange =
    minDate.year..maxDate.year

/** The selectable months for [year], narrowed at the two boundary years so a date outside
 * [minDate]..[maxDate] can never be constructed from the wheels. */
internal fun monthWheelRange(year: Int, minDate: LocalDate, maxDate: LocalDate): IntRange {
    val lo = if (year == minDate.year) minDate.monthValue else 1
    val hi = if (year == maxDate.year) maxDate.monthValue else 12
    return lo..hi
}

/** The selectable days for [year]/[month], narrowed the same way [monthWheelRange] narrows
 * months - and always clamped to that month's actual length, so e.g. a 31-day boundary month
 * never offers a day past its own end. */
internal fun dayWheelRange(year: Int, month: Int, minDate: LocalDate, maxDate: LocalDate): IntRange {
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val lo = if (year == minDate.year && month == minDate.monthValue) minDate.dayOfMonth else 1
    val hi = if (year == maxDate.year && month == maxDate.monthValue) {
        maxDate.dayOfMonth.coerceAtMost(daysInMonth)
    } else {
        daysInMonth
    }
    return lo..hi
}

/** Pulls [date] into [minDate]..[maxDate] if it falls outside - used to seed the wheels from a
 * caller-supplied initial date without requiring every caller to pre-clamp it themselves. */
internal fun clampDateToBounds(date: LocalDate, minDate: LocalDate, maxDate: LocalDate): LocalDate = when {
    date.isBefore(minDate) -> minDate
    date.isAfter(maxDate) -> maxDate
    else -> date
}
