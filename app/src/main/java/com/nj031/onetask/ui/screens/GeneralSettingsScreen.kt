package com.nj031.onetask.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.data.settings.TimeFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Settings > General, reachable from the hamburger drawer's "General" row (previously a no-op).
 * Every row is fully functional except Appearance, which opens a shared "coming soon" screen,
 * the same treatment Privacy Policy already got before its content existed.
 */
@Composable
fun GeneralSettingsScreen(
    startScreen: StartScreen,
    weekStartDay: DayOfWeek,
    timeFormat: TimeFormat,
    onBackClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onStartScreenClick: () -> Unit,
    onDefaultTaskSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onWeekStartsOnClick: () -> Unit,
    onTimeFormatClick: () -> Unit,
    onHapticFeedbackClick: () -> Unit
) {
    val startScreenLabel = stringResource(
        id = if (startScreen == StartScreen.TASKS) R.string.start_screen_option_tasks else R.string.start_screen_option_journal
    )
    val weekStartDayLabel = weekStartDay.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val timeFormatLabel = stringResource(
        id = when (timeFormat) {
            TimeFormat.SYSTEM_DEFAULT -> R.string.time_format_system_default
            TimeFormat.HOUR_12 -> R.string.time_format_12_hour
            TimeFormat.HOUR_24 -> R.string.time_format_24_hour
        }
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(id = R.string.general_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            GeneralSettingsRow(
                title = stringResource(id = R.string.general_appearance),
                modifier = Modifier.padding(top = 20.dp),
                onClick = onAppearanceClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_start_screen),
                value = startScreenLabel,
                onClick = onStartScreenClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_default_task_settings),
                onClick = onDefaultTaskSettingsClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_notifications),
                onClick = onNotificationsClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_week_starts_on),
                value = weekStartDayLabel,
                onClick = onWeekStartsOnClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_time_format),
                value = timeFormatLabel,
                onClick = onTimeFormatClick
            )
            GeneralSettingsRow(
                title = stringResource(id = R.string.general_haptic_feedback),
                onClick = onHapticFeedbackClick
            )
        }
    }
}

@Composable
private fun GeneralSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
