package com.nj031.onetask.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Journal : Screen("journal")
    data object Archive : Screen("archive")
    data object RecycleBin : Screen("recycle_bin")
    data object NoteEditor : Screen("note_editor?noteId={noteId}") {
        fun createRoute(noteId: String? = null): String =
            if (noteId != null) "note_editor?noteId=$noteId" else "note_editor"
    }
}
