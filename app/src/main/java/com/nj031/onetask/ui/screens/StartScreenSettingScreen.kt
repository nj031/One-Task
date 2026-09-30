package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.data.settings.Wallpaper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource

/**
 * General Settings > Start Screen. Tasks/Timer/Notes are the three launch destinations - Tasks
 * is the default for both existing and new users, matching the app's launch behavior before this
 * setting existed. The selected value is written through immediately on tap, no separate "Save"
 * step. [StartScreen.JOURNAL] is the underlying enum constant's name (kept for backward
 * compatibility with values already persisted on existing installs) but is labeled "Notes"
 * everywhere in this UI, matching the section that replaced the old Journal.
 */
@Composable
fun StartScreenSettingScreen(
    selected: StartScreen,
    onSelect: (StartScreen) -> Unit,
    onBackClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    if (wallpaper == Wallpaper.WALLPAPER_1) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_wp1_back),
                            contentDescription = stringResource(id = R.string.back),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.start_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.start_screen_description),
                style = MaterialTheme.typography.bodyMedium,
                // This sits directly on the raw wallpaper background (no Card), so
                // onSurfaceVariant (tuned for an opaque Card) reads poorly under Wallpaper 1 -
                // fall back to the same dimmed onBackground treatment used for other
                // raw-background secondary text elsewhere in the app. Every other theme is
                // unaffected.
                color = if (wallpaper != Wallpaper.NONE) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp).selectableGroup()) {
                    StartScreenOptionRow(
                        label = stringResource(id = R.string.start_screen_option_tasks),
                        selected = selected == StartScreen.TASKS,
                        onClick = { onSelect(StartScreen.TASKS) }
                    )
                    StartScreenOptionRow(
                        label = stringResource(id = R.string.start_screen_option_timer),
                        selected = selected == StartScreen.TIMER,
                        onClick = { onSelect(StartScreen.TIMER) }
                    )
                    StartScreenOptionRow(
                        label = stringResource(id = R.string.start_screen_option_journal),
                        selected = selected == StartScreen.JOURNAL,
                        onClick = { onSelect(StartScreen.JOURNAL) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartScreenOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            // onSurface (not onBackground) - this row sits on the Card's own opaque surface
            // fill, not the raw background.
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
