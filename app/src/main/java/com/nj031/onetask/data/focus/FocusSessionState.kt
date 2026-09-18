package com.nj031.onetask.data.focus

import android.content.Context
import com.nj031.onetask.data.UserScopedPreferences

/**
 * Tracks which task (if any) currently has an open, not-yet-exited Focus Mode session, purely so
 * the app can relaunch straight back into Focus Mode after its process has been killed in the
 * background - Compose Navigation's own back-stack restoration only covers an Activity being
 * recreated while its saved-state Bundle survives, not every real-world "reopen the app" path
 * (e.g. the OS fully discarding the task, or an OEM background killer).
 *
 * This is deliberately kept separate from the task's own timer/status fields in Room: pausing
 * from *inside* Focus Mode (the Pause button) leaves the exact same persisted task state as
 * Break/Leave Focus (timer paused, still IN_PROGRESS, remaining time preserved), but only the
 * latter two should end the session - so which one happened can only be tracked by recording
 * whether the screen itself is still open, not derived from the task record alone. Whenever
 * this disagrees with reality (e.g. the referenced task was completed or deleted through some
 * other path while not observed), FocusTimerScreen's own state already reconciles it - a
 * completed task auto-navigates home (clearing this), and a missing task simply renders nothing.
 */
object FocusSessionState {
    private const val PREFS_NAME = "focus_session_state"
    private const val KEY_ACTIVE_TASK_ID = "active_task_id"

    fun getActiveTaskId(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE_TASK_ID, null)

    fun setActive(context: Context, taskId: String) {
        prefs(context).edit().putString(KEY_ACTIVE_TASK_ID, taskId).apply()
    }

    fun clearActive(context: Context) {
        prefs(context).edit().remove(KEY_ACTIVE_TASK_ID).apply()
    }

    // Resolved fresh on every call (never cached) and scoped per signed-in account (see
    // UserScopedPreferences) - a plain fixed file name would mean every account on this device
    // shared which task (if any) had an open Focus Mode session, and a stale task id from a
    // different account could otherwise be read back here across a sign-out/sign-in.
    private fun prefs(context: Context) = UserScopedPreferences.open(context, PREFS_NAME)
}
