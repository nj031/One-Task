package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nj031.onetask.R
import com.nj031.onetask.ui.components.BottomNavTab
import com.nj031.onetask.ui.components.OneTaskBottomNav

@Composable
fun ProfileScreen(
    onNavigateToJournal: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OneTaskBottomNav(
                activeTab = BottomNavTab.PROFILE,
                onJournalClick = onNavigateToJournal,
                onTasksClick = onNavigateToTasks,
                onProfileClick = {}
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.profile_under_construction),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}
