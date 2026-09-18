package com.nj031.onetask.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager clears every scheduled alarm on a device reboot - this restores Task Reminders
 * for whichever account is signed in (Firebase Auth's own session persists across a reboot, so
 * AuthRepository.currentUser is already correct by the time this runs) without requiring the
 * user to reopen the app first. See [ReminderManager.rescheduleAll]. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderManager.rescheduleAll(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
