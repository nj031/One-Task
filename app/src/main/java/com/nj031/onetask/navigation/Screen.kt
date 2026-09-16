package com.nj031.onetask.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Journal : Screen("journal")
    data object Profile : Screen("profile")
    data object Archive : Screen("archive")
    data object RecycleBin : Screen("recycle_bin")
    data object NoteEditor : Screen("note_editor?noteId={noteId}") {
        fun createRoute(noteId: String? = null): String =
            if (noteId != null) "note_editor?noteId=$noteId" else "note_editor"
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
}
