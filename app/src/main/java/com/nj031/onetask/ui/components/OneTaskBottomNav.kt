package com.nj031.onetask.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R

enum class BottomNavTab { JOURNAL, TASKS, TIMER }

/**
 * The One Task bottom navigation bar (Tasks / Timer / Notes), shared by every screen
 * that's reachable from it, so it's always visible and consistent instead of disappearing
 * when navigating between those screens. The middle tab is an intentionally empty placeholder
 * screen - Profile & Settings is reached separately, via the avatar icon in the Tasks/Notes
 * top bars, not through this bottom nav.
 *
 * The three tab icons are the exact supplied bitmap assets (R.drawable.ic_nav_*_active /
 * _inactive, cropped directly from the provided reference sheet), not custom-drawn vectors -
 * swapped by [selected] state. The active variant is additionally tinted to the current theme's
 * primary color (its baked-in artwork was drawn blue, so a non-blue color theme still needs a
 * runtime recolor here) - the inactive variant's neutral gray is intentionally left untouched by
 * theming, matching every other "unselected" element elsewhere in the app.
 */
@Composable
fun OneTaskBottomNav(
    activeTab: BottomNavTab,
    onJournalClick: () -> Unit,
    onTasksClick: () -> Unit,
    onTimerClick: () -> Unit,
    // Defaults to the ordinary theme surface color, exactly as before this param existed - only
    // a wallpaper-active screen (see WallpaperBackdrop) ever passes its own distinct translucent
    // Bottom Navigation color instead (see the Wallpaper spec's own exact opacity values, kept
    // separate from every other card-like surface's own translucency).
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val journalLabel = stringResource(id = R.string.nav_journal)
    val tasksLabel = stringResource(id = R.string.nav_tasks)
    val timerLabel = stringResource(id = R.string.nav_timer)

    // The system navigation area (3-button bar or gesture bar) draws on top of app
    // content since the app opts into edge-to-edge. windowInsetsPadding here keeps the
    // actual nav items entirely above that area on both navigation modes, while the
    // surface background still extends all the way down behind it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OneTaskBottomNavItem(
                icon = {
                    val tasksSelected = activeTab == BottomNavTab.TASKS
                    Image(
                        painter = painterResource(
                            id = if (tasksSelected) R.drawable.ic_nav_tasks_active else R.drawable.ic_nav_tasks_inactive
                        ),
                        contentDescription = null,
                        colorFilter = if (tasksSelected) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = tasksLabel,
                onClick = onTasksClick,
                selected = activeTab == BottomNavTab.TASKS
            )
            OneTaskBottomNavItem(
                icon = {
                    val timerSelected = activeTab == BottomNavTab.TIMER
                    Image(
                        painter = painterResource(
                            id = if (timerSelected) R.drawable.ic_nav_timer_active else R.drawable.ic_nav_timer_inactive
                        ),
                        contentDescription = null,
                        colorFilter = if (timerSelected) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = timerLabel,
                onClick = onTimerClick,
                selected = activeTab == BottomNavTab.TIMER
            )
            OneTaskBottomNavItem(
                icon = {
                    val notesSelected = activeTab == BottomNavTab.JOURNAL
                    Image(
                        painter = painterResource(
                            id = if (notesSelected) R.drawable.ic_nav_notes_active else R.drawable.ic_nav_notes_inactive
                        ),
                        contentDescription = null,
                        colorFilter = if (notesSelected) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = journalLabel,
                onClick = onJournalClick,
                selected = activeTab == BottomNavTab.JOURNAL
            )
        }
    }
}

@Composable
private fun OneTaskBottomNavItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    selected: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
