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

/**
 * THE single canonical time-of-day picker for the whole One Task app - the source of truth for
 * "pick a time of day" everywhere it's needed (currently Add Task's Reminder time; any future
 * feature with the same need should call this rather than building another one). Always a
 * 12-hour Hour/Minute/AM-PM wheel picker per the locked product design - this deliberately
 * replaces the Material3 clock/dial `TimePicker` this app used to show here, which is no longer
 * the canonical UI for this purpose.
 *
 * This is a different component from [OneTaskDurationPickerDialog]: that one picks a *length* of
 * time (hr/min/sec, no AM/PM), this one picks a *point* in the day (hour/min + AM/PM, no
 * seconds) - the two are kept separate since they answer different questions and have no
 * meaningful value range or validation in common.
 *
 * [initialMinuteOfDay] and the callback's result are both minutes-since-midnight (0..1439, the
 * same representation [com.nj031.onetask.data.task.TaskEntity.reminderMinuteOfDay] already
 * stores) - callers never need to think in 12-hour/AM-PM terms outside this dialog.
 */
@Composable
fun OneTaskTimePickerDialog(
    initialMinuteOfDay: Int?,
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    // 9:00 AM is a plain, unsurprising default for a time that hasn't been picked yet - any
    // validation of that resulting time (e.g. "not in the past") is the caller's responsibility,
    // exactly as it already was before this component existed.
    val initialState = remember(initialMinuteOfDay) {
        minuteOfDayToWheelState(initialMinuteOfDay ?: (9 * 60))
    }
    var pickedHour12 by remember { mutableStateOf(initialState.hour12) }
    var pickedMinute by remember { mutableStateOf(initialState.minute) }
    var pickedIsPm by remember { mutableStateOf(initialState.isPm) }

    val hourLabels = remember { (1..12).map(Int::toString) }
    val minuteLabels = remember { (0..59).map { "%02d".format(it) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.time_picker_title),
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
                        items = hourLabels,
                        selectedIndex = pickedHour12 - 1,
                        onSelectedIndexChange = { pickedHour12 = it + 1 },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = minuteLabels,
                        selectedIndex = pickedMinute,
                        onSelectedIndexChange = { pickedMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = listOf(
                            stringResource(id = R.string.time_picker_am),
                            stringResource(id = R.string.time_picker_pm)
                        ),
                        selectedIndex = if (pickedIsPm) 1 else 0,
                        onSelectedIndexChange = { pickedIsPm = it == 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            onTimeSelected(wheelStateToMinuteOfDay(pickedHour12, pickedMinute, pickedIsPm))
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.timer_custom_duration_confirm),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/** [hour12] is 1..12 (never 0), [minute] is 0..59, [isPm] false means AM. */
internal data class TimeWheelState(val hour12: Int, val minute: Int, val isPm: Boolean)

/** Converts a 0..1439 minutes-since-midnight value into the 12-hour wheel representation
 * [OneTaskTimePickerDialog] displays. Pure and unit-tested - see TimeWheelStateTest. */
internal fun minuteOfDayToWheelState(minuteOfDay: Int): TimeWheelState {
    val hour24 = (minuteOfDay / 60) % 24
    val minute = minuteOfDay % 60
    val isPm = hour24 >= 12
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return TimeWheelState(hour12, minute, isPm)
}

/** The inverse of [minuteOfDayToWheelState] - converts a wheel selection back into a 0..1439
 * minutes-since-midnight value, the representation every caller stores/compares against. */
internal fun wheelStateToMinuteOfDay(hour12: Int, minute: Int, isPm: Boolean): Int {
    val hour24 = when {
        hour12 == 12 && !isPm -> 0
        hour12 == 12 && isPm -> 12
        isPm -> hour12 + 12
        else -> hour12
    }
    return hour24 * 60 + minute
}
