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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nj031.onetask.R
import com.nj031.onetask.data.timer.MIN_TIMER_DURATION_MILLIS

/**
 * THE single canonical Hours/Minutes/Seconds duration-selection dialog for the whole One Task
 * app - the source of truth for "pick a custom duration" everywhere it's needed (the Timer
 * screen's own Custom Duration, Add Task's Timer field, Default Task Settings' Default Timer, and
 * any future feature with the same need). Do not copy this file's contents into a new screen;
 * call this composable instead.
 *
 * [minDurationMillis] is the floor below which the confirm button stays disabled - the Timer
 * screen's own long-standing 5-second minimum by default. A caller whose stored value only has
 * minute precision (see [durationMillisToWholeMinutes]) should pass at least 60_000L so a
 * 0-minute "custom" duration is never confirmable.
 */
@Composable
fun OneTaskDurationPickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    minDurationMillis: Long = MIN_TIMER_DURATION_MILLIS
) {
    val initialTotalSeconds = (initialMillis / 1000).coerceAtLeast(0)
    var pickedHours by remember { mutableStateOf((initialTotalSeconds / 3600).toInt().coerceIn(0, 99)) }
    var pickedMinutes by remember { mutableStateOf(((initialTotalSeconds % 3600) / 60).toInt().coerceIn(0, 59)) }
    var pickedSeconds by remember { mutableStateOf((initialTotalSeconds % 60).toInt().coerceIn(0, 59)) }

    val hourLabels = remember { (0..99).map { "%02d".format(it) } }
    val minuteOrSecondLabels = remember { (0..59).map { "%02d".format(it) } }

    val totalPickedMillis = (pickedHours * 3_600L + pickedMinutes * 60L + pickedSeconds) * 1_000L
    val isValid = totalPickedMillis >= minDurationMillis

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
                        text = stringResource(id = R.string.timer_custom_duration_title),
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
                        selectedIndex = pickedHours,
                        onSelectedIndexChange = { pickedHours = it },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = minuteOrSecondLabels,
                        selectedIndex = pickedMinutes,
                        onSelectedIndexChange = { pickedMinutes = it },
                        modifier = Modifier.weight(1f)
                    )
                    DateWheelColumnDivider()
                    DateWheelColumn(
                        items = minuteOrSecondLabels,
                        selectedIndex = pickedSeconds,
                        onSelectedIndexChange = { pickedSeconds = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(
                        text = stringResource(id = R.string.timer_unit_hr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.timer_unit_min),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.timer_unit_sec),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        .then(if (isValid) Modifier.clickable { onConfirm(totalPickedMillis) } else Modifier)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.timer_custom_duration_confirm),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Converts a duration picked via [OneTaskDurationPickerDialog] into whole minutes, rounding to
 * the nearest minute - for the two callers (Add Task's Timer field, Default Task Settings'
 * Default Timer) whose stored value is a minute-only Int and always has been; the picker itself
 * always shows all three (hr/min/sec) wheels per the canonical design, so this is where that
 * extra seconds precision is deliberately collapsed rather than being silently stored (which
 * would either truncate without the caller knowing, or require widening the existing minute-based
 * data model just to accommodate the shared picker UI).
 */
fun durationMillisToWholeMinutes(millis: Long): Int =
    Math.round(millis / 60_000.0).toInt().coerceAtLeast(0)
