package com.nj031.onetask.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.feedback.FeedbackType
import com.nj031.onetask.ui.screens.AddTaskScreen
import com.nj031.onetask.ui.screens.ArchiveScreen
import com.nj031.onetask.ui.screens.AuthScreen
import com.nj031.onetask.ui.screens.DataPrivacyScreen
import com.nj031.onetask.ui.screens.EditProfileScreen
import com.nj031.onetask.ui.screens.FeedbackFormScreen
import com.nj031.onetask.ui.screens.FocusTimerScreen
import com.nj031.onetask.ui.screens.HelpFaqCategoryScreen
import com.nj031.onetask.ui.screens.HelpFaqScreen
import com.nj031.onetask.ui.screens.HelpFeedbackScreen
import com.nj031.onetask.ui.screens.HomeScreen
import com.nj031.onetask.ui.screens.JournalScreen
import com.nj031.onetask.ui.screens.NoteEditorScreen
import com.nj031.onetask.ui.screens.PrivacyPolicyScreen
import com.nj031.onetask.ui.screens.ProfileScreen
import com.nj031.onetask.ui.screens.RecycleBinScreen
import com.nj031.onetask.ui.screens.UpgradeToProScreen
import com.nj031.onetask.viewmodel.AuthViewModel
import com.nj031.onetask.viewmodel.HomeViewModel
import com.nj031.onetask.viewmodel.JournalViewModel
import com.nj031.onetask.viewmodel.ProfileViewModel

/**
 * Standard "bottom nav" navigation: switching between the Homepage/Journal/Profile tabs
 * reuses a single saved instance of each rather than stacking a new one on every tap, so
 * repeatedly tapping between tabs doesn't grow the back stack unbounded.
 */
private fun NavHostController.navigateToBottomNavTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun OneTaskNavHost(
    navController: NavHostController = rememberNavController(),
    activeFocusTaskId: String? = null
) {
    val journalViewModel: JournalViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val startDestination = when {
        AuthRepository.currentUser == null -> Screen.Auth.route
        activeFocusTaskId != null -> Screen.FocusTimer.createRoute(activeFocusTaskId)
        else -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel()
            val context = LocalContext.current
            val isLoading by authViewModel.isLoading.collectAsState()
            val errorMessage by authViewModel.errorMessage.collectAsState()
            AuthScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSignInWithGoogle = {
                    authViewModel.signInWithGoogle(context) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToJournal = { navController.navigateToBottomNavTab(Screen.Journal.route) },
                onNavigateToProfile = { navController.navigateToBottomNavTab(Screen.Profile.route) },
                onOpenFocusTimer = { taskId ->
                    navController.navigate(Screen.FocusTimer.createRoute(taskId))
                },
                onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute()) },
                onEditTaskClick = { taskId ->
                    navController.navigate(Screen.AddTask.createRoute(taskId))
                },
                onArchiveClick = { navController.navigate(Screen.Archive.route) },
                onRecycleBinClick = { navController.navigate(Screen.RecycleBin.route) },
                onDataPrivacyClick = { navController.navigate(Screen.DataPrivacy.route) },
                onUpgradeToProClick = { navController.navigate(Screen.UpgradeToPro.route) },
                onHelpFeedbackClick = { navController.navigate(Screen.HelpFeedback.route) },
                onLogout = {
                    AuthRepository.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(
            route = Screen.AddTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            AddTaskScreen(
                viewModel = homeViewModel,
                taskId = backStackEntry.arguments?.getString("taskId"),
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.FocusTimer.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            FocusTimerScreen(
                taskId = backStackEntry.arguments?.getString("taskId").orEmpty(),
                onBackToHome = {
                    // A plain popBackStack() only works when Home is actually sitting somewhere
                    // below Focus Mode in the back stack - true for the normal Homepage-opened-
                    // Focus-Mode flow, but NOT when Focus Mode was restored as the app's cold
                    // start destination (an active session surviving a killed process): there,
                    // Focus Mode IS the graph's root entry, so there's nothing beneath it to pop
                    // to and popBackStack() would silently do nothing, leaving the user stuck on
                    // a screen (Break/Leave Focus) that's supposed to have just closed. Try the
                    // normal pop first - it preserves the existing Home instance's state - and
                    // only fall back to a fresh, fully-reset navigation to Home if there was
                    // nothing to pop to.
                    val poppedToHome = navController.popBackStack(Screen.Home.route, false)
                    if (!poppedToHome) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
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
                onDataPrivacyClick = { navController.navigate(Screen.DataPrivacy.route) },
                onUpgradeToProClick = { navController.navigate(Screen.UpgradeToPro.route) },
                onHelpFeedbackClick = { navController.navigate(Screen.HelpFeedback.route) },
                onNavigateToTasks = { navController.navigateToBottomNavTab(Screen.Home.route) },
                onNavigateToProfile = { navController.navigateToBottomNavTab(Screen.Profile.route) },
                onLogout = {
                    AuthRepository.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateToJournal = { navController.navigateToBottomNavTab(Screen.Journal.route) },
                onNavigateToTasks = { navController.navigateToBottomNavTab(Screen.Home.route) },
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onUpgradeToProClick = { navController.navigate(Screen.UpgradeToPro.route) }
            )
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                viewModel = profileViewModel,
                onDone = { navController.popBackStack() }
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
        composable(Screen.DataPrivacy.route) {
            DataPrivacyScreen(
                onBackClick = { navController.popBackStack() },
                onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onAccountDeleted = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.UpgradeToPro.route) {
            UpgradeToProScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.HelpFeedback.route) {
            HelpFeedbackScreen(
                onBackClick = { navController.popBackStack() },
                onHelpFaqClick = { navController.navigate(Screen.HelpFaq.route) },
                onSuggestFeatureClick = {
                    navController.navigate(Screen.FeedbackForm.createRoute(FeedbackType.FEATURE.backendValue))
                },
                onReportProblemClick = {
                    navController.navigate(Screen.FeedbackForm.createRoute(FeedbackType.BUG.backendValue))
                },
                onSendFeedbackClick = {
                    navController.navigate(Screen.FeedbackForm.createRoute(FeedbackType.FEEDBACK.backendValue))
                }
            )
        }
        composable(Screen.HelpFaq.route) {
            HelpFaqScreen(
                onBackClick = { navController.popBackStack() },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.HelpFaqCategory.createRoute(categoryId))
                }
            )
        }
        composable(
            route = Screen.HelpFaqCategory.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            HelpFaqCategoryScreen(
                categoryId = backStackEntry.arguments?.getString("categoryId").orEmpty(),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.FeedbackForm.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeArg = backStackEntry.arguments?.getString("type").orEmpty()
            FeedbackFormScreen(
                type = FeedbackType.fromRouteValue(typeArg),
                onBackClick = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
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
