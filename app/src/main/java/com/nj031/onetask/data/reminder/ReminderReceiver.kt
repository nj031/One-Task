package com.nj031.onetask.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nj031.onetask.MainActivity
import com.nj031.onetask.R
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.UserScope
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepeat
import com.nj031.onetask.data.task.TaskStatus
import com.nj031.onetask.data.task.asVirtualOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val REMINDER_CHANNEL_ID = "task_reminder_channel_v1"

/**
 * Fires when a Task Reminder's AlarmManager alarm goes off. Re-verifies everything live against
 * Room rather than trusting what was true when the alarm was scheduled - the task may have been
 * completed, edited, deleted, or the signed-in account may have changed - since this can run a
 * long time (hours to months, for a recurring series) after [ReminderManager.reschedule] last ran.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderManager.ACTION_TASK_REMINDER) return
        val taskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID) ?: return
        val scheduledUid = intent.getStringExtra(ReminderManager.EXTRA_UID) ?: return
        val epochDay = intent.getLongExtra(ReminderManager.EXTRA_EPOCH_DAY, -1L)
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handleReminderFired(appContext, taskId, scheduledUid, epochDay)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminderFired(context: Context, taskId: String, scheduledUid: String, epochDay: Long) {
        // Account isolation: this alarm was scheduled by whichever account was signed in at the
        // time - if a different account is signed in now (or nobody is), it must never surface
        // this reminder, whatever the currently-active database happens to contain.
        if (UserScope.id() != scheduledUid) return

        val dao = AppDatabase.getInstance(context).taskDao()
        val task = dao.getById(taskId).first() ?: return
        if (task.reminderMinuteOfDay == null) return

        val alreadyCompleted = if (task.repeat == TaskRepeat.NONE) {
            task.status == TaskStatus.COMPLETED
        } else {
            val occurrenceId = task.asVirtualOccurrence(epochDay).id
            dao.getById(occurrenceId).first()?.status == TaskStatus.COMPLETED
        }
        if (!alreadyCompleted) {
            postNotification(context, task)
        }
        // Whether or not this occurrence showed a notification, advance the schedule to the next
        // applicable one so a recurring series always keeps exactly one alarm outstanding.
        ReminderManager.reschedule(context, task)
    }

    private fun postNotification(context: Context, task: TaskEntity) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TASK_ID, task.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(task.name)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notification_open,
                    context.getString(R.string.reminder_notification_open_action),
                    openIntent
                ).build()
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // No notification permission - the reminder was still processed correctly (schedule
            // advanced above); there's simply nothing visible to show for it.
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
}
