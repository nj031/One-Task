package com.nj031.onetask.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.UserScope
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat

/**
 * Owns every AlarmManager call Task Reminders makes - schedules, reschedules, and cancels the
 * single outstanding alarm a task/recurring series can have (see [ReminderScheduler]), and the
 * account-wide resync used on app start and after a device reboot (alarms don't survive a
 * reboot, so [rescheduleAll] is also invoked from ReminderBootReceiver).
 *
 * Deliberately keyed only by a task's own id, never by a specific recurring occurrence: a
 * materialized occurrence row ([TaskEntity.seriesId] != null) never schedules or cancels
 * anything on its own - only the series' own definition row (or a plain one-time task) does, so
 * there's always at most one alarm per series, self-rescheduled to the next applicable,
 * not-yet-completed occurrence every time it fires (see ReminderReceiver) or the task is edited.
 */
object ReminderManager {
    const val ACTION_TASK_REMINDER = "com.nj031.onetask.action.TASK_REMINDER"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_UID = "uid"
    const val EXTRA_EPOCH_DAY = "epoch_day"

    /** Cancels any existing alarm for [task] and, unless it's a materialized occurrence (which
     * never independently schedules one), computes and schedules the next applicable one. Safe
     * to call after every task mutation that could affect its reminder (create, edit, complete,
     * un-complete) - a no-op beyond the cancel when there's nothing left to schedule. */
    suspend fun reschedule(context: Context, task: TaskEntity) {
        cancel(context, task.id)
        if (task.seriesId != null) return
        val dao = AppDatabase.getInstance(context).taskDao()
        val completedDates = if (task.repeat != TaskRepeat.NONE) {
            dao.getCompletedOccurrenceDatesForSeries(task.id).toSet()
        } else {
            emptySet()
        }
        val occurrence = ReminderScheduler.computeNextOccurrence(
            task = task,
            fromEpochMillis = System.currentTimeMillis(),
            completedOccurrenceDates = completedDates
        ) ?: return
        scheduleAlarm(context, task.id, occurrence)
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildPendingIntent(context, taskId, create = false) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Re-derives and reschedules every reminder for the currently signed-in account from its
     * own persisted task data - the recovery path after a device reboot (AlarmManager clears all
     * alarms on one) and the resync run on every app start/fresh sign-in, so a missed reschedule
     * never permanently loses a reminder. No-ops when nobody is signed in. */
    suspend fun rescheduleAll(context: Context) {
        if (AuthRepository.currentUser == null) return
        val dao = AppDatabase.getInstance(context).taskDao()
        dao.getAll()
            .filter { it.seriesId == null && it.reminderMinuteOfDay != null }
            .forEach { reschedule(context, it) }
    }

    /** Cancels every currently-scheduled reminder belonging to whichever account is signed in
     * right now - must be called BEFORE AuthRepository.signOut(), while AppDatabase.getInstance
     * still resolves to the outgoing account's own database (see UserScope/AppDatabase), so the
     * next account signed into on this device can never receive a leftover reminder from this
     * one (account isolation - the same requirement the rest of this app's per-account storage
     * already enforces for Tasks/Notes/Settings). */
    suspend fun cancelAllForCurrentAccount(context: Context) {
        if (AuthRepository.currentUser == null) return
        val dao = AppDatabase.getInstance(context).taskDao()
        dao.getAll()
            .filter { it.seriesId == null && it.reminderMinuteOfDay != null }
            .forEach { cancel(context, it.id) }
    }

    private fun scheduleAlarm(context: Context, taskId: String, occurrence: ReminderOccurrence) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildPendingIntent(context, taskId, create = true, epochDay = occurrence.epochDay)
            ?: return
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    occurrence.fireAtEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.fireAtEpochMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Exact-alarm scheduling permission was revoked (Settings) between the check above
            // and this call, or an OEM quirk disagrees with canScheduleExactAlarms() - fall back
            // to an inexact-but-still-delivered alarm rather than lose the reminder or crash.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.fireAtEpochMillis, pendingIntent)
        }
    }

    /** [create] false performs a lookup-only match (FLAG_NO_CREATE) for cancellation - intent
     * extras never factor into PendingIntent matching, only the action/component/request code
     * below do, so this reliably finds the exact same PendingIntent [scheduleAlarm] registered
     * regardless of which occurrence it was last scheduled for. */
    private fun buildPendingIntent(
        context: Context,
        taskId: String,
        create: Boolean,
        epochDay: Long = 0L
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_UID, UserScope.id())
            putExtra(EXTRA_EPOCH_DAY, epochDay)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, taskId.hashCode(), intent, flags)
    }
}
