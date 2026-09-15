package com.nj031.onetask.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nj031.onetask.ui.screens.ArchiveScreen
import com.nj031.onetask.ui.screens.AuthScreen
import com.nj031.onetask.ui.screens.FocusTimerScreen
import com.nj031.onetask.ui.screens.HomeScreen
import com.nj031.onetask.ui.screens.JournalScreen
import com.nj031.onetask.ui.screens.NoteEditorScreen
import com.nj031.onetask.ui.screens.RecycleBinScreen
import com.nj031.onetask.viewmodel.JournalViewModel

@Composable
fun OneTaskNavHost(navController: NavHostController = rememberNavController()) {
    val journalViewModel: JournalViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val navigateHome = { navController.navigate(Screen.Home.route) }
            AuthScreen(
                onLogIn = navigateHome,
                onContinueWithGoogle = navigateHome,
                onCreateAccount = navigateHome
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                onOpenFocusTimer = { taskId ->
                    navController.navigate(Screen.FocusTimer.createRoute(taskId))
                }
            )
        }
        composable(
            route = Screen.FocusTimer.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            FocusTimerScreen(
                taskId = backStackEntry.arguments?.getString("taskId").orEmpty(),
                onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                onBackToHome = { navController.popBackStack() }
            )
        }
        composable(Screen.Journal.route) {
            JournalScreen(
                viewModel = journalViewModel,
                onAddNoteClick = { navController.navigate(Screen.NoteEditor.createRoute()) },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onRecycleBinClick = { navController.navigate(Screen.RecycleBin.route) },
                onArchiveClick = { navController.navigate(Screen.Archive.route) },
                onSettingsClick = { /* no-op: settings not implemented yet */ }
            )
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(
                viewModel = journalViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.RecycleBin.route) {
            RecycleBinScreen(
                viewModel = journalViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            NoteEditorScreen(
                viewModel = journalViewModel,
                noteId = backStackEntry.arguments?.getString("noteId"),
                onDone = { navController.popBackStack() }
            )
        }
    }
}
