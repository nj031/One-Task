package com.nj031.onetask.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Login : Screen("login")
    data object CreateAccountEmail : Screen("create_account_email")
    data object CreateAccountPassword : Screen("create_account_password")
    data object VerifyEmail : Screen("verify_email")
    data object SetUpProfile : Screen("set_up_profile")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object Journal : Screen("journal")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    /** The bottom nav's third tab (replacing the old Profile tab): a Timer/Stopwatch screen with
     * a segmented Timer/Stopwatch control - see TimerPlaceholderScreen. */
    data object TimerPlaceholder : Screen("timer_placeholder")
    data object TimerSettings : Screen("timer_settings")
    data object TimerCustomDurationSettings : Screen("timer_custom_duration_settings")
    data object TimerHistory : Screen("timer_history")
    data object Archive : Screen("archive")
    data object RecycleBin : Screen("recycle_bin")
    data object Labels : Screen("labels")
    data object NoteEditor : Screen("note_editor?noteId={noteId}&noteType={noteType}") {
        fun createRoute(noteId: String? = null, noteType: String = "TEXT"): String {
            val idPart = if (noteId != null) "noteId=$noteId&" else ""
            return "note_editor?${idPart}noteType=$noteType"
        }
    }
    data object FocusTimer : Screen("focus_timer/{taskId}") {
        fun createRoute(taskId: String): String = "focus_timer/$taskId"
    }
    data object AddTask : Screen("add_task?taskId={taskId}") {
        fun createRoute(taskId: String? = null): String =
            if (taskId != null) "add_task?taskId=$taskId" else "add_task"
    }
    data object DataPrivacy : Screen("data_privacy")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object UpgradeToPro : Screen("upgrade_to_pro")
    data object HelpFeedback : Screen("help_feedback")
    data object HelpFaq : Screen("help_faq")
    data object HelpFaqCategory : Screen("help_faq_category/{categoryId}") {
        fun createRoute(categoryId: String): String = "help_faq_category/$categoryId"
    }
    data object FeedbackForm : Screen("feedback_form/{type}") {
        fun createRoute(type: String): String = "feedback_form/$type"
    }
    data object GeneralSettings : Screen("general_settings")
    data object AppearanceSettings : Screen("appearance_settings")
    data object StartScreenSettings : Screen("start_screen_settings")
    data object CategoriesSettings : Screen("categories_settings")
    data object DefaultTaskSettings : Screen("default_task_settings")
    data object NotificationsSettings : Screen("notifications_settings")
    data object WeekStartsOnSettings : Screen("week_starts_on_settings")
    data object TimeFormatSettings : Screen("time_format_settings")
    data object HapticFeedbackSettings : Screen("haptic_feedback_settings")
    data object AboutOneTask : Screen("about_one_task")
    data object TermsOfService : Screen("terms_of_service")
    data object OpenSourceLicenses : Screen("open_source_licenses")
}
