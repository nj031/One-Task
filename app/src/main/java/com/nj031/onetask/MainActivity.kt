package com.nj031.onetask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nj031.onetask.data.focus.FocusSessionState
import com.nj031.onetask.navigation.OneTaskNavHost
import com.nj031.onetask.ui.theme.OneTaskTheme

class MainActivity : ComponentActivity() {
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
            OneTaskTheme {
                OneTaskNavHost(activeFocusTaskId = activeFocusTaskId)
            }
        }
    }
}
