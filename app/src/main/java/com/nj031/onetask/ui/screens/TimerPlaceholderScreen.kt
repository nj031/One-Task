package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.OneTaskBottomNav

/**
 * The bottom nav's third tab, replacing the old Profile tab. Deliberately blank - a placeholder
 * for a future Timer feature, with no functionality of its own yet.
 */
@Composable
fun TimerPlaceholderScreen(
    onNavigateToJournal: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OneTaskBottomNav(
                activeTab = BottomNavTab.TIMER,
                onJournalClick = onNavigateToJournal,
                onTasksClick = onNavigateToTasks,
                onTimerClick = {}
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
