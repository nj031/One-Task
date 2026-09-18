package com.nj031.onetask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.data.focus.FocusSessionState
import com.nj031.onetask.data.settings.DisplayMode
import com.nj031.onetask.navigation.OneTaskNavHost
import com.nj031.onetask.ui.theme.OneTaskTheme
import com.nj031.onetask.viewmodel.AppearanceSettingsViewModel

class MainActivity : ComponentActivity() {
    // Bridges the running Focus Timer notification's "Open" action into a WARM app (process
    // already alive, Activity just not currently showing Focus Mode): onNewIntent below writes
    // into this Compose state from outside composition, which OneTaskNavHost observes to
    // explicitly navigate there. A cold start doesn't need this at all - it's handled entirely
    // by activeFocusTaskId/FocusSessionState below, exactly as before this existed. requestId
    // only exists so a second tap for the SAME task (taskId unchanged) still re-triggers
    // navigation rather than being ignored as a no-op state write.
    private val reopenFocusTaskId = mutableStateOf<String?>(null)
    private val reopenFocusRequestId = mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Read synchronously, before the first frame: if a Focus Mode session was left open
        // (i.e. not exited via Break/Leave Focus/completion) the last time the app ran, this
        // Activity instance must start directly on Focus Mode rather than momentarily showing
        // the Homepage - this covers the process having been killed in the background, not just
        // an in-process Activity recreation (which Navigation Compose already restores itself).
        val activeFocusTaskId = FocusSessionState.getActiveTaskId(this)
        setContent {
            val reopenTaskId by reopenFocusTaskId
            val reopenRequestId by reopenFocusRequestId

            // Hoisted here (above OneTaskTheme) rather than inside OneTaskNavHost, since the
            // Display Mode/Color Theme selection has to be known before OneTaskTheme itself is
            // entered - the same instance is then threaded down into OneTaskNavHost so the
            // Appearance screen, deep in the nav graph, mutates this exact ViewModel rather than
            // a separate NavBackStackEntry-scoped one.
            val appearanceSettingsViewModel: AppearanceSettingsViewModel = viewModel()
            val displayMode by appearanceSettingsViewModel.displayMode.collectAsState()
            val colorTheme by appearanceSettingsViewModel.colorTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (displayMode) {
                DisplayMode.SYSTEM -> systemInDarkTheme
                DisplayMode.LIGHT -> false
                DisplayMode.DARK -> true
            }

            OneTaskTheme(darkTheme = darkTheme, colorTheme = colorTheme) {
                OneTaskNavHost(
                    activeFocusTaskId = activeFocusTaskId,
                    reopenFocusTaskId = reopenTaskId,
                    reopenFocusRequestId = reopenRequestId,
                    appearanceSettingsViewModel = appearanceSettingsViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_FOCUS_TASK_ID)?.let { taskId ->
            reopenFocusTaskId.value = taskId
            reopenFocusRequestId.value += 1
        }
    }

    companion object {
        const val EXTRA_OPEN_FOCUS_TASK_ID = "open_focus_task_id"
    }
}
