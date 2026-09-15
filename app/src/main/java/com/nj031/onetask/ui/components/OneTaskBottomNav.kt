package com.nj031.onetask.ui.components

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.ui.theme.OneTaskJournalIcon
import com.nj031.onetask.ui.theme.OneTaskProfileIcon
import com.nj031.onetask.ui.theme.OneTaskTasksIcon

enum class BottomNavTab { JOURNAL, TASKS, PROFILE }

/**
 * The One Task bottom navigation bar (Journal / Tasks / Profile), shared by every screen
 * that's reachable from it, so it's always visible and consistent instead of disappearing
 * when navigating between those screens.
 */
@Composable
fun OneTaskBottomNav(
    activeTab: BottomNavTab,
    onJournalClick: () -> Unit,
    onTasksClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val journalLabel = stringResource(id = R.string.nav_journal)
    val tasksLabel = stringResource(id = R.string.nav_tasks)
    val profileLabel = stringResource(id = R.string.nav_profile)

    // The system navigation area (3-button bar or gesture bar) draws on top of app
    // content since the app opts into edge-to-edge. windowInsetsPadding here keeps the
    // actual nav items entirely above that area on both navigation modes, while the
    // surface background still extends all the way down behind it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
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
                    OneTaskJournalIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
                },
                label = journalLabel,
                onClick = onJournalClick,
                selected = activeTab == BottomNavTab.JOURNAL
            )
            OneTaskBottomNavItem(
                icon = { OneTaskTasksIcon(active = activeTab == BottomNavTab.TASKS, size = 28.dp) },
                label = tasksLabel,
                onClick = onTasksClick,
                selected = activeTab == BottomNavTab.TASKS
            )
            OneTaskBottomNavItem(
                icon = {
                    OneTaskProfileIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
                },
                label = profileLabel,
                onClick = onProfileClick,
                selected = activeTab == BottomNavTab.PROFILE
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
