package com.nj031.onetask.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nj031.onetask.R
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.feedback.FeedbackType
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.ui.haptics.LocalHapticFeedbackEnabled
import com.nj031.onetask.ui.screens.AboutOneTaskScreen
import com.nj031.onetask.ui.screens.AddTaskScreen
import com.nj031.onetask.ui.screens.ArchiveScreen
import com.nj031.onetask.ui.screens.AuthScreen
import com.nj031.onetask.ui.screens.CreateAccountEmailScreen
import com.nj031.onetask.ui.screens.CreateAccountPasswordScreen
import com.nj031.onetask.ui.screens.DataPrivacyScreen
import com.nj031.onetask.ui.screens.DefaultTaskSettingsScreen
import com.nj031.onetask.ui.screens.EditProfileScreen
import com.nj031.onetask.ui.screens.FeedbackFormScreen
import com.nj031.onetask.ui.screens.FocusTimerScreen
import com.nj031.onetask.ui.screens.ForgotPasswordScreen
import com.nj031.onetask.ui.screens.GeneralSettingsScreen
import com.nj031.onetask.ui.screens.HapticFeedbackSettingScreen
import com.nj031.onetask.ui.screens.HelpFaqCategoryScreen
import com.nj031.onetask.ui.screens.HelpFaqScreen
import com.nj031.onetask.ui.screens.HelpFeedbackScreen
import com.nj031.onetask.ui.screens.HomeScreen
import com.nj031.onetask.ui.screens.LabelsScreen
import com.nj031.onetask.ui.screens.LoginScreen
import com.nj031.onetask.ui.screens.NoteEditorScreen
import com.nj031.onetask.ui.screens.NotesScreen
import com.nj031.onetask.ui.screens.NotificationsSettingsScreen
import com.nj031.onetask.ui.screens.PrivacyPolicyScreen
import com.nj031.onetask.ui.screens.ProfileScreen
import com.nj031.onetask.ui.screens.RecycleBinScreen
import com.nj031.onetask.ui.screens.SettingsComingSoonScreen
import com.nj031.onetask.ui.screens.SetUpProfileScreen
import com.nj031.onetask.ui.screens.StartScreenSettingScreen
import com.nj031.onetask.ui.screens.TimeFormatSettingScreen
import com.nj031.onetask.ui.screens.TimerPlaceholderScreen
import com.nj031.onetask.ui.screens.UpgradeToProScreen
import com.nj031.onetask.ui.screens.VerifyEmailScreen
import com.nj031.onetask.ui.screens.WeekStartsOnSettingScreen
import com.nj031.onetask.viewmodel.AuthViewModel
import com.nj031.onetask.viewmodel.GeneralSettingsViewModel
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
    activeFocusTaskId: String? = null,
    reopenFocusTaskId: String? = null,
    reopenFocusRequestId: Long = 0L
) {
    // A warm reopen via the running Focus Timer notification's "Open" action (see
    // MainActivity.onNewIntent): navigates straight to Focus Mode over whatever screen was
    // showing. Keyed on reopenFocusRequestId (not reopenFocusTaskId) so a second tap for the
    // same task still re-triggers this, and guarded to a no-op on the very first composition
    // (requestId 0) so a cold start - already handled below by activeFocusTaskId as the graph's
    // startDestination - never double-navigates on top of itself.
    LaunchedEffect(reopenFocusRequestId) {
        if (reopenFocusRequestId == 0L) return@LaunchedEffect
        val taskId = reopenFocusTaskId ?: return@LaunchedEffect
        navController.navigate(Screen.FocusTimer.createRoute(taskId)) {
            launchSingleTop = true
        }
    }

    val journalViewModel: JournalViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val generalSettingsViewModel: GeneralSettingsViewModel = viewModel()
    val hapticFeedbackEnabled by generalSettingsViewModel.hapticFeedbackEnabled.collectAsState()
    val startDestination = when {
        AuthRepository.currentUser == null -> Screen.Auth.route
        // Only an unverified account can reach this point: Google sign-in accounts are always
        // pre-verified by Firebase, and an unverified email/password account only exists here
        // because the app was killed mid-signup, after the account was created but before its
        // link was confirmed - resume exactly where they left off instead of dropping them on
        // Home with an unverified account.
        !AuthRepository.isCurrentUserEmailVerified -> Screen.VerifyEmail.route
        // An in-progress Focus session always takes priority over the Start Screen setting -
        // the user is mid-task, not just launching the app fresh.
        activeFocusTaskId != null -> Screen.FocusTimer.createRoute(activeFocusTaskId)
        generalSettingsViewModel.startScreen.value == StartScreen.JOURNAL -> Screen.Journal.route
        generalSettingsViewModel.startScreen.value == StartScreen.TIMER -> Screen.TimerPlaceholder.route
        else -> Screen.Home.route
    }

    /** After a successful login/signup, verified accounts always land on Home; the ambiguous
     * "just verified email, still needs a name" case never reaches here directly, since
     * Verify Email's own onContinue keeps that decision local. */
    fun NavHostController.navigateToHomeAfterAuth() {
        navigate(Screen.Home.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
        }
    }

    CompositionLocalProvider(LocalHapticFeedbackEnabled provides hapticFeedbackEnabled) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            val context = LocalContext.current
            val isLoading by authViewModel.isLoading.collectAsState()
            val errorMessage by authViewModel.errorMessage.collectAsState()
            AuthScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onCreateAccount = { navController.navigate(Screen.CreateAccountEmail.route) },
                onLogIn = { navController.navigate(Screen.Login.route) },
                onSignInWithGoogle = {
                    authViewModel.signInWithGoogle(context) { needsProfileSetup ->
                        if (needsProfileSetup) {
                            navController.navigate(Screen.SetUpProfile.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        } else {
                            navController.navigateToHomeAfterAuth()
                        }
                    }
                }
            )
        }
        composable(Screen.CreateAccountEmail.route) {
            val signUpState by authViewModel.signUpState.collectAsState()
            CreateAccountEmailScreen(
                state = signUpState,
                onEmailChange = authViewModel::updateSignUpEmail,
                onNext = {
                    authViewModel.submitSignUpEmail {
                        navController.navigate(Screen.CreateAccountPassword.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CreateAccountPassword.route) {
            val signUpState by authViewModel.signUpState.collectAsState()
            CreateAccountPasswordScreen(
                state = signUpState,
                onPasswordChange = authViewModel::updateSignUpPassword,
                onConfirmPasswordChange = authViewModel::updateSignUpConfirmPassword,
                onNext = {
                    authViewModel.submitSignUpPassword {
                        navController.navigate(Screen.VerifyEmail.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VerifyEmail.route) {
            val signUpState by authViewModel.signUpState.collectAsState()
            val verifyEmailState by authViewModel.verifyEmailState.collectAsState()
            VerifyEmailScreen(
                email = signUpState.email,
                state = verifyEmailState,
                onResend = authViewModel::resendVerificationEmail,
                onContinue = {
                    authViewModel.checkEmailVerified {
                        val displayName = AuthRepository.currentUser?.displayName
                        if (displayName.isNullOrBlank()) {
                            navController.navigate(Screen.SetUpProfile.route)
                        } else {
                            navController.navigateToHomeAfterAuth()
                        }
                    }
                },
                onBack = {
                    // Verification -> Password per spec, but this screen can also be the app's
                    // cold-start root (a killed, mid-signup process resumed here) with nothing
                    // beneath it to pop to - same defensive pattern as FocusTimerScreen's
                    // onBackToHome, so Back can never unexpectedly close the app.
                    val poppedToPassword = navController.popBackStack(Screen.CreateAccountPassword.route, false)
                    if (!poppedToPassword) {
                        AuthRepository.signOut()
                        navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
        composable(Screen.SetUpProfile.route) {
            val profileSetupState by authViewModel.profileSetupState.collectAsState()
            SetUpProfileScreen(
                state = profileSetupState,
                onNameChange = authViewModel::updateProfileSetupName,
                onFinish = {
                    authViewModel.submitProfileSetup {
                        navController.navigateToHomeAfterAuth()
                    }
                },
                onBack = {
                    // Profile setup -> Verification for the email signup chain; a Google
                    // signup with no usable name skips straight from Main Login to here, so
                    // fall back to a plain pop (-> Main Login) when Verification isn't in the
                    // stack, and only as a last resort (this screen resumed as the cold-start
                    // root) sign out rather than leave Back with nothing to do.
                    val poppedToVerify = navController.popBackStack(Screen.VerifyEmail.route, false)
                    if (!poppedToVerify && !navController.popBackStack()) {
                        AuthRepository.signOut()
                        navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            val loginState by authViewModel.loginState.collectAsState()
            LoginScreen(
                state = loginState,
                onEmailChange = authViewModel::updateLoginEmail,
                onPasswordChange = authViewModel::updateLoginPassword,
                onLogIn = {
                    authViewModel.submitLogin {
                        val user = AuthRepository.currentUser
                        when {
                            !AuthRepository.isCurrentUserEmailVerified -> navController.navigate(Screen.VerifyEmail.route) {
                                popUpTo(Screen.Auth.route) { inclusive = false }
                            }
                            user?.displayName.isNullOrBlank() -> navController.navigate(Screen.SetUpProfile.route) {
                                popUpTo(Screen.Auth.route) { inclusive = false }
                            }
                            else -> navController.navigateToHomeAfterAuth()
                        }
                    }
                },
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            val forgotPasswordState by authViewModel.forgotPasswordState.collectAsState()
            ForgotPasswordScreen(
                state = forgotPasswordState,
                onEmailChange = authViewModel::updateForgotPasswordEmail,
                onSendResetLink = authViewModel::submitForgotPassword,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            val weekStartDay by generalSettingsViewModel.weekStartDay.collectAsState()
            val profile by profileViewModel.profile.collectAsState()
            HomeScreen(
                viewModel = homeViewModel,
                profilePhotoPath = profile.photoPath,
                onNavigateToJournal = { navController.navigateToBottomNavTab(Screen.Journal.route) },
                onOpenTimerPlaceholder = { navController.navigateToBottomNavTab(Screen.TimerPlaceholder.route) },
                onProfileAvatarClick = { navController.navigate(Screen.Profile.route) },
                onOpenFocusTimer = { taskId ->
                    navController.navigate(Screen.FocusTimer.createRoute(taskId))
                },
                onAddTaskClick = { navController.navigate(Screen.AddTask.createRoute()) },
                onEditTaskClick = { taskId ->
                    navController.navigate(Screen.AddTask.createRoute(taskId))
                },
                weekStartDay = weekStartDay
            )
        }
        composable(Screen.TimerPlaceholder.route) {
            TimerPlaceholderScreen(
                onNavigateToJournal = { navController.navigateToBottomNavTab(Screen.Journal.route) },
                onNavigateToTasks = { navController.navigateToBottomNavTab(Screen.Home.route) }
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
            val defaultTimerMinutes by generalSettingsViewModel.defaultTimerMinutes.collectAsState()
            val defaultTag by generalSettingsViewModel.defaultTag.collectAsState()
            val defaultPostponeIfIncomplete by generalSettingsViewModel.defaultPostponeIfIncomplete.collectAsState()
            val weekStartDay by generalSettingsViewModel.weekStartDay.collectAsState()
            AddTaskScreen(
                viewModel = homeViewModel,
                taskId = backStackEntry.arguments?.getString("taskId"),
                defaultTimerMinutes = defaultTimerMinutes,
                defaultTag = defaultTag,
                defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
                weekStartDay = weekStartDay,
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
            val timeFormat by generalSettingsViewModel.timeFormat.collectAsState()
            val profile by profileViewModel.profile.collectAsState()
            NotesScreen(
                viewModel = journalViewModel,
                profilePhotoPath = profile.photoPath,
                onAddNoteClick = { noteType ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteType = noteType.name))
                },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onNavigateToTasks = { navController.navigateToBottomNavTab(Screen.Home.route) },
                onOpenTimerPlaceholder = { navController.navigateToBottomNavTab(Screen.TimerPlaceholder.route) },
                onProfileAvatarClick = { navController.navigate(Screen.Profile.route) },
                onArchiveClick = { navController.navigate(Screen.Archive.route) },
                onRecycleBinClick = { navController.navigate(Screen.RecycleBin.route) },
                onLabelsClick = { navController.navigate(Screen.Labels.route) },
                timeFormat = timeFormat
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() },
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onUpgradeToProClick = { navController.navigate(Screen.UpgradeToPro.route) },
                onGeneralSettingsClick = { navController.navigate(Screen.GeneralSettings.route) },
                onDataPrivacyClick = { navController.navigate(Screen.DataPrivacy.route) },
                onAboutClick = { navController.navigate(Screen.AboutOneTask.route) },
                onHelpFeedbackClick = { navController.navigate(Screen.HelpFeedback.route) },
                onLogout = {
                    AuthRepository.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0)
                    }
                }
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
        composable(Screen.Labels.route) {
            LabelsScreen(
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
        composable(Screen.AboutOneTask.route) {
            AboutOneTaskScreen(
                onBackClick = { navController.popBackStack() },
                onTermsOfServiceClick = { navController.navigate(Screen.TermsOfService.route) },
                onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onOpenSourceLicensesClick = { navController.navigate(Screen.OpenSourceLicenses.route) }
            )
        }
        composable(Screen.TermsOfService.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.terms_of_service_title),
                message = stringResource(id = R.string.terms_of_service_placeholder),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.OpenSourceLicenses.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.open_source_licenses_title),
                message = stringResource(id = R.string.open_source_licenses_placeholder),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.GeneralSettings.route) {
            val startScreen by generalSettingsViewModel.startScreen.collectAsState()
            val weekStartDay by generalSettingsViewModel.weekStartDay.collectAsState()
            val timeFormat by generalSettingsViewModel.timeFormat.collectAsState()
            GeneralSettingsScreen(
                startScreen = startScreen,
                weekStartDay = weekStartDay,
                timeFormat = timeFormat,
                onBackClick = { navController.popBackStack() },
                onAppearanceClick = { navController.navigate(Screen.AppearanceSettings.route) },
                onStartScreenClick = { navController.navigate(Screen.StartScreenSettings.route) },
                onDefaultTaskSettingsClick = { navController.navigate(Screen.DefaultTaskSettings.route) },
                onNotificationsClick = { navController.navigate(Screen.NotificationsSettings.route) },
                onWeekStartsOnClick = { navController.navigate(Screen.WeekStartsOnSettings.route) },
                onTimeFormatClick = { navController.navigate(Screen.TimeFormatSettings.route) },
                onHapticFeedbackClick = { navController.navigate(Screen.HapticFeedbackSettings.route) }
            )
        }
        composable(Screen.AppearanceSettings.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.appearance_title),
                message = stringResource(id = R.string.appearance_placeholder_message),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.StartScreenSettings.route) {
            val startScreen by generalSettingsViewModel.startScreen.collectAsState()
            StartScreenSettingScreen(
                selected = startScreen,
                onSelect = generalSettingsViewModel::setStartScreen,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.DefaultTaskSettings.route) {
            val defaultTimerMinutes by generalSettingsViewModel.defaultTimerMinutes.collectAsState()
            val defaultTag by generalSettingsViewModel.defaultTag.collectAsState()
            val defaultPostponeIfIncomplete by generalSettingsViewModel.defaultPostponeIfIncomplete.collectAsState()
            val customTags by homeViewModel.customTags.collectAsState()
            DefaultTaskSettingsScreen(
                defaultTimerMinutes = defaultTimerMinutes,
                defaultTag = defaultTag,
                defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
                customTags = customTags,
                onDefaultTimerMinutesChange = generalSettingsViewModel::setDefaultTimerMinutes,
                onDefaultTagChange = generalSettingsViewModel::setDefaultTag,
                onDefaultPostponeIfIncompleteChange = generalSettingsViewModel::setDefaultPostponeIfIncomplete,
                onAddCustomTag = homeViewModel::addCustomTag,
                onDeleteCustomTag = homeViewModel::deleteCustomTag,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.NotificationsSettings.route) {
            val focusSessionNotificationsEnabled by generalSettingsViewModel.focusSessionNotificationsEnabled.collectAsState()
            val focusSessionCompleteEnabled by generalSettingsViewModel.focusSessionCompleteEnabled.collectAsState()
            NotificationsSettingsScreen(
                focusSessionNotificationsEnabled = focusSessionNotificationsEnabled,
                focusSessionCompleteEnabled = focusSessionCompleteEnabled,
                onFocusSessionNotificationsChange = generalSettingsViewModel::setFocusSessionNotificationsEnabled,
                onFocusSessionCompleteChange = generalSettingsViewModel::setFocusSessionCompleteEnabled,
                onTaskRemindersClick = { navController.navigate(Screen.UpgradeToPro.route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.WeekStartsOnSettings.route) {
            val weekStartDay by generalSettingsViewModel.weekStartDay.collectAsState()
            WeekStartsOnSettingScreen(
                selected = weekStartDay,
                onSelect = generalSettingsViewModel::setWeekStartDay,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.TimeFormatSettings.route) {
            val timeFormat by generalSettingsViewModel.timeFormat.collectAsState()
            TimeFormatSettingScreen(
                selected = timeFormat,
                onSelect = generalSettingsViewModel::setTimeFormat,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.HapticFeedbackSettings.route) {
            val hapticEnabled by generalSettingsViewModel.hapticFeedbackEnabled.collectAsState()
            HapticFeedbackSettingScreen(
                enabled = hapticEnabled,
                onEnabledChange = generalSettingsViewModel::setHapticFeedbackEnabled,
                onBackClick = { navController.popBackStack() }
            )
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
                },
                navArgument("noteType") {
                    type = NavType.StringType
                    defaultValue = JournalNoteType.TEXT.name
                }
            )
        ) { backStackEntry ->
            val noteTypeArg = backStackEntry.arguments?.getString("noteType")
            NoteEditorScreen(
                viewModel = journalViewModel,
                noteId = backStackEntry.arguments?.getString("noteId"),
                noteType = runCatching { JournalNoteType.valueOf(noteTypeArg ?: "TEXT") }
                    .getOrDefault(JournalNoteType.TEXT),
                onDone = { navController.popBackStack() }
            )
        }
    }
    }
}
