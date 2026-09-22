package com.nj031.onetask.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nj031.onetask.MainActivity
import com.nj031.onetask.R
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.feedback.FeedbackType
import com.nj031.onetask.data.journal.JournalNoteType
import com.nj031.onetask.data.reminder.ReminderManager
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.haptics.LocalHapticFeedbackEnabled
import com.nj031.onetask.ui.screens.AboutOneTaskScreen
import com.nj031.onetask.ui.screens.AddTaskScreen
import com.nj031.onetask.ui.screens.AppearanceSettingsScreen
import com.nj031.onetask.ui.screens.ArchiveScreen
import com.nj031.onetask.ui.screens.AuthScreen
import com.nj031.onetask.ui.screens.CategoriesScreen
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
import com.nj031.onetask.service.StandaloneTimerForegroundService
import com.nj031.onetask.service.TimerForegroundService
import com.nj031.onetask.ui.screens.WeekStartsOnSettingScreen
import com.nj031.onetask.ui.theme.WallpaperSettingsTheme
import com.nj031.onetask.viewmodel.AddTaskDraftViewModel
import com.nj031.onetask.viewmodel.AppearanceSettingsViewModel
import com.nj031.onetask.viewmodel.AuthViewModel
import com.nj031.onetask.viewmodel.GeneralSettingsViewModel
import com.nj031.onetask.viewmodel.HomeViewModel
import com.nj031.onetask.viewmodel.JournalViewModel
import com.nj031.onetask.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

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
    reopenFocusRequestId: Long = 0L,
    reopenTaskId: String? = null,
    reopenTaskRequestId: Long = 0L,
    appearanceSettingsViewModel: AppearanceSettingsViewModel = viewModel(),
    wallpaper: Wallpaper = Wallpaper.NONE,
    darkTheme: Boolean = false
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

    // A Task Reminder notification's tap/"Open app" action, cold or warm alike (see
    // MainActivity.onCreate/onNewIntent - both set reopenTaskRequestId to a non-zero value the
    // very first time this composes, unlike reopenFocusRequestId above which only needs to
    // handle a warm reopen). Navigates to the same Edit Task screen tapping that task's own card
    // on Home would.
    LaunchedEffect(reopenTaskRequestId) {
        if (reopenTaskRequestId == 0L) return@LaunchedEffect
        val taskId = reopenTaskId ?: return@LaunchedEffect
        navController.navigate(Screen.AddTask.createRoute(taskId)) {
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val journalViewModel: JournalViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    // Hoisted here (not inside the AddTask composable() block below) for the same reason
    // homeViewModel is - see AddTaskDraftViewModel's own doc comment - so the in-progress Add/
    // Edit Task form survives a round trip to Settings' Custom Category management and back.
    val addTaskDraftViewModel: AddTaskDraftViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val generalSettingsViewModel: GeneralSettingsViewModel = viewModel()
    val hapticFeedbackEnabled by generalSettingsViewModel.hapticFeedbackEnabled.collectAsState()

    /**
     * Where a signed-in, verified user belongs right now - an in-progress Focus session, the
     * Start Screen preference, or plain Home - and Auth/VerifyEmail for anyone not fully signed
     * in yet. Used below as [startDestination], the graph's initial route - evaluated fresh on
     * every new Activity instance (including the one [restartToFreshSession] starts right after
     * a sign-in/sign-out), so it always reflects AuthRepository.currentUser as of that instance's
     * own first composition.
     */
    fun resolveAuthenticatedDestination(): String = when {
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

    val startDestination = resolveAuthenticatedDestination()

    // Guards restartToFreshSession() against firing twice for one tap - a fast double-tap on
    // the sign-in button or the logout confirm button would otherwise call it again before the
    // first call's startActivity()/finish() has actually taken the old Activity off screen.
    var accountTransitionStarted by remember { mutableStateOf(false) }

    // Guards endSessionAndReturnToAuth() itself against a double-tap launching its cleanup
    // coroutine twice - kept as a SEPARATE flag from accountTransitionStarted (rather than
    // reusing it) so that setting this one early never pre-empts restartToFreshSession()'s own
    // guard/call at the end of that coroutine: reusing accountTransitionStarted here used to mark
    // the restart as "already started" before restartToFreshSession() ever ran, so its own
    // `if (accountTransitionStarted) return` always fired first and the actual
    // startActivity()/finish() restart never executed on the logout path, even though
    // AuthRepository.signOut() had already completed.
    var logoutStarted by remember { mutableStateOf(false) }

    /**
     * Fully restarts the app into a brand-new task/Activity instance - the mechanism every
     * account transition (sign-in success, sign-out) relies on to guarantee every ViewModel this
     * NavHost hoists, and every repository/DAO handle those ViewModels cache, is genuinely
     * reconstructed fresh and correctly bound to whoever is signed in now.
     *
     * This intentionally does NOT use Activity.recreate() with a manually cleared
     * ViewModelStore, which is what PR #96 originally tried and which crashed on both sign-in
     * and sign-out with `IllegalStateException: ViewModelStore should be set before setGraph
     * call` (thrown from NavController.setViewModelStore, via NavHost) - Navigation Compose
     * keeps its own internal NavControllerViewModel inside this same Activity ViewModelStore,
     * and forcibly clearing that store from application code while NavHost/NavController for it
     * is still alive and composing breaks an internal invariant neither library documents as
     * safe to violate. recreate() alone (without the manual clear) doesn't fix the underlying
     * account-isolation problem either: it's documented to follow essentially the same flow as a
     * configuration change, which is exactly the mechanism ComponentActivity uses to RETAIN (not
     * discard) its ViewModelStore across rotation - so a hoisted ViewModel would simply survive
     * into the next signed-in account unchanged.
     *
     * A full task restart (FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK) sidesteps both
     * problems at once, using only ordinary, fully-supported Activity lifecycle behavior:
     * - The new Activity instance gets a genuinely NEW ViewModelStore. onRetainNonConfiguration-
     *   Instance() (the mechanism that retains a ViewModelStore across recreate()/rotation) is
     *   never consulted for a normal finish() - the old Activity's ViewModelStore is cleared by
     *   the framework itself, as part of its own ordinary teardown, never by this code reaching
     *   into a store a live NavController still owns.
     * - The new task has no saved Compose Navigation back-stack Bundle to restore, so the fresh
     *   NavHost's own `startDestination` (computed above from AuthRepository.currentUser at that
     *   point) is used correctly - unlike recreate(), which restores whatever route was on
     *   screen the moment it was called, which is what caused sign-in to silently bounce back to
     *   the login screen before this fix.
     */
    fun restartToFreshSession() {
        if (accountTransitionStarted) return
        accountTransitionStarted = true
        val activity = context as? Activity ?: return
        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        activity.finish()
    }

    /** After a successful login/signup, verified accounts always land on Home (or wherever the
     * Start Screen setting/an in-progress Focus session points); the ambiguous "just verified
     * email, still needs a name" case never reaches here directly, since Verify Email's own
     * onContinue keeps that decision local. See [restartToFreshSession] for why this restarts
     * the app rather than simply navigating. */
    fun navigateToHomeAfterAuth() {
        restartToFreshSession()
    }

    /**
     * Signs out (unless the account was already deleted, which signs itself out) and restarts
     * into a fresh session for the same account-isolation reason [restartToFreshSession] does -
     * so no ViewModel (or cached repository/DAO handle) hoisted for the just-signed-out account
     * can still be sitting in memory, ready to serve its data, whenever the next sign-in happens.
     * Also stops any background Timer/Stopwatch/Focus session service: those run independently
     * of this Activity's lifecycle (see TimerForegroundService/StandaloneTimerForegroundService,
     * both `stopWithTask="false"`) and each caches its own account-scoped repository once at
     * onCreate() - left running, one would keep reading/writing the just-signed-out account's
     * timer state (and showing it in a system notification) straight through the next account's
     * session.
     */
    fun endSessionAndReturnToAuth(alreadySignedOut: Boolean = false) {
        // Set synchronously, before the coroutine below's first suspension point, so a rapid
        // double-tap can't launch this twice - restartToFreshSession()'s own guard only takes
        // effect once this whole coroutine actually reaches it, which is too late to stop a
        // second concurrent call from starting its own cancel-and-sign-out pass first. Uses its
        // own logoutStarted flag (not accountTransitionStarted) precisely so it doesn't disarm
        // restartToFreshSession()'s later call - see logoutStarted's own comment above.
        if (logoutStarted) return
        logoutStarted = true
        // Cancels every one of the outgoing account's scheduled Task Reminders BEFORE signing
        // out, while AppDatabase.getInstance still resolves to that account's own database (see
        // ReminderManager.cancelAllForCurrentAccount) - otherwise a leftover alarm could still
        // fire and show that account's task name to whoever signs into this device next, which
        // is exactly the kind of cross-account leak this app's per-account storage architecture
        // otherwise already prevents everywhere else. Launched rather than awaited inline since
        // this is a plain (non-suspend) callback; restartToFreshSession/finish() only happens
        // once this coroutine's own cancellation work has actually completed.
        coroutineScope.launch {
            // A no-op (AuthRepository.currentUser already null) when the account was signed out
            // by something else before this ran (e.g. DataPrivacyScreen's account-deletion flow,
            // alreadySignedOut = true) - cross-account isolation is still guaranteed either way,
            // since ReminderReceiver independently re-checks the signed-in uid at fire time.
            ReminderManager.cancelAllForCurrentAccount(context)
            if (!alreadySignedOut) {
                AuthRepository.signOut()
            }
            context.stopService(Intent(context, TimerForegroundService::class.java))
            StandaloneTimerForegroundService.stop(context)
            restartToFreshSession()
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
                            navigateToHomeAfterAuth()
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
                            navigateToHomeAfterAuth()
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
                        endSessionAndReturnToAuth()
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
                        navigateToHomeAfterAuth()
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
                        endSessionAndReturnToAuth()
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
                            else -> navigateToHomeAfterAuth()
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
                onAddCategoryClick = { navController.navigate(Screen.CategoriesSettings.route) },
                weekStartDay = weekStartDay,
                wallpaper = wallpaper,
                darkTheme = darkTheme
            )
        }
        composable(Screen.TimerPlaceholder.route) {
            TimerPlaceholderScreen(
                onNavigateToJournal = { navController.navigateToBottomNavTab(Screen.Journal.route) },
                onNavigateToTasks = { navController.navigateToBottomNavTab(Screen.Home.route) },
                onNotificationSettingsClick = { navController.navigate(Screen.NotificationsSettings.route) },
                onTimerSettingsClick = { navController.navigate(Screen.TimerSettings.route) },
                onCustomDurationSettingsClick = { navController.navigate(Screen.TimerCustomDurationSettings.route) },
                onTimerHistoryClick = { navController.navigate(Screen.TimerHistory.route) },
                wallpaper = wallpaper,
                darkTheme = darkTheme
            )
        }
        composable(Screen.TimerSettings.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.timer_menu_settings),
                message = stringResource(id = R.string.timer_settings_placeholder),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.TimerCustomDurationSettings.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.timer_menu_custom_duration),
                message = stringResource(id = R.string.timer_custom_duration_settings_placeholder),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.TimerHistory.route) {
            SettingsComingSoonScreen(
                title = stringResource(id = R.string.timer_menu_history),
                message = stringResource(id = R.string.timer_history_placeholder),
                onBackClick = { navController.popBackStack() }
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
            val timeFormat by generalSettingsViewModel.timeFormat.collectAsState()
            val customCategories by homeViewModel.customCategories.collectAsState()
            AddTaskScreen(
                viewModel = homeViewModel,
                draftViewModel = addTaskDraftViewModel,
                taskId = backStackEntry.arguments?.getString("taskId"),
                customCategories = customCategories,
                onAddCategoryClick = { navController.navigate(Screen.CategoriesSettings.route) },
                defaultTimerMinutes = defaultTimerMinutes,
                defaultTag = defaultTag,
                defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
                weekStartDay = weekStartDay,
                timeFormat = timeFormat,
                onDone = {
                    // See AddTaskDraftViewModel's own doc comment - only Save/Cancel actually
                    // leaving this screen clears the draft; navigating to Settings (above) does
                    // not, since that's a mid-flow round trip, not leaving.
                    addTaskDraftViewModel.clear()
                    navController.popBackStack()
                }
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
                timeFormat = timeFormat,
                wallpaper = wallpaper,
                darkTheme = darkTheme
            )
        }
        composable(Screen.Profile.route) {
            WallpaperSettingsTheme(wallpaper = wallpaper, darkTheme = darkTheme) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                    onUpgradeToProClick = { navController.navigate(Screen.UpgradeToPro.route) },
                    onGeneralSettingsClick = { navController.navigate(Screen.GeneralSettings.route) },
                    onDataPrivacyClick = { navController.navigate(Screen.DataPrivacy.route) },
                    onAboutClick = { navController.navigate(Screen.AboutOneTask.route) },
                    onHelpFeedbackClick = { navController.navigate(Screen.HelpFeedback.route) },
                    onLogout = { endSessionAndReturnToAuth() }
                )
            }
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
                onAccountDeleted = { endSessionAndReturnToAuth(alreadySignedOut = true) }
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
                onCategoriesClick = { navController.navigate(Screen.CategoriesSettings.route) },
                onDefaultTaskSettingsClick = { navController.navigate(Screen.DefaultTaskSettings.route) },
                onNotificationsClick = { navController.navigate(Screen.NotificationsSettings.route) },
                onWeekStartsOnClick = { navController.navigate(Screen.WeekStartsOnSettings.route) },
                onTimeFormatClick = { navController.navigate(Screen.TimeFormatSettings.route) },
                onHapticFeedbackClick = { navController.navigate(Screen.HapticFeedbackSettings.route) }
            )
        }
        composable(Screen.AppearanceSettings.route) {
            val displayMode by appearanceSettingsViewModel.displayMode.collectAsState()
            val colorTheme by appearanceSettingsViewModel.colorTheme.collectAsState()
            val currentWallpaper by appearanceSettingsViewModel.wallpaper.collectAsState()
            WallpaperSettingsTheme(wallpaper = currentWallpaper, darkTheme = darkTheme) {
                AppearanceSettingsScreen(
                    displayMode = displayMode,
                    colorTheme = colorTheme,
                    wallpaper = currentWallpaper,
                    darkTheme = darkTheme,
                    onSelectDisplayMode = appearanceSettingsViewModel::setDisplayMode,
                    onSelectColorTheme = appearanceSettingsViewModel::setColorTheme,
                    onSelectWallpaper = appearanceSettingsViewModel::setWallpaper,
                    onBackClick = { navController.popBackStack() }
                )
            }
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
            val defaultPostponeIfIncomplete by generalSettingsViewModel.defaultPostponeIfIncomplete.collectAsState()
            DefaultTaskSettingsScreen(
                defaultTimerMinutes = defaultTimerMinutes,
                defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
                onDefaultTimerMinutesChange = generalSettingsViewModel::setDefaultTimerMinutes,
                onDefaultPostponeIfIncompleteChange = generalSettingsViewModel::setDefaultPostponeIfIncomplete,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.CategoriesSettings.route) {
            val customCategories by homeViewModel.customCategories.collectAsState()
            CategoriesScreen(
                customCategories = customCategories,
                onAddCustomCategory = homeViewModel::addCustomCategory,
                onRenameCustomCategory = homeViewModel::renameCustomCategory,
                onDeleteCustomCategory = homeViewModel::deleteCustomCategory,
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
                onDone = { navController.popBackStack() },
                onManageLabelsClick = { navController.navigate(Screen.Labels.route) }
            )
        }
    }
    }
}
