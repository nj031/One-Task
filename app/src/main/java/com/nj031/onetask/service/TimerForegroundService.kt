package com.nj031.onetask.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nj031.onetask.MainActivity
import com.nj031.onetask.R
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.task.TaskEntity
import com.nj031.onetask.data.task.TaskRepository
import com.nj031.onetask.data.task.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owns a running focus-timer session as a foreground service so it keeps counting down (and
 * shows a persistent notification) while the app is backgrounded or the screen is locked.
 *
 * This service holds no timer state of its own - the single source of truth is the task's row
 * in Room (specifically its absolute timerEndAtMillis end-timestamp). The service just observes
 * that row: it stops itself the moment the task is no longer an actively-running timer (paused,
 * reset, deleted, marked done, or its duration edited to something already elapsed), and detects
 * the countdown reaching zero even while nothing is on screen, immediately stopping the timer in
 * the same way the UI would.
 */
class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null
    private lateinit var taskRepository: TaskRepository

    override fun onCreate() {
        super.onCreate()
        taskRepository = TaskRepository(AppDatabase.getInstance(applicationContext).taskDao())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (taskId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID_RUNNING, buildRunningNotification(taskName = "", remainingLabel = "--:--"))
        observeTask(taskId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeTask(taskId: String) {
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            taskRepository.observeTaskById(taskId).collectLatest { task ->
                // Checked first and unconditionally: this is the one point every path through a
                // natural completion passes through - whether the service's own countdown below
                // reached zero, or the UI's tick loop got there first while this row was still
                // being observed - so it's the single reliable place to fire the one-shot
                // "session complete" notification, however the app was being used at the time.
                if (task != null && isJustCompleted(task)) {
                    postCompletionNotification(task)
                }
                if (task == null || task.status != TaskStatus.IN_PROGRESS || task.timerEndAtMillis == null) {
                    stopSelf()
                    return@collectLatest
                }
                runCountdown(task)
            }
        }
    }

    /**
     * True only for the exact instant a timer has just stopped at zero and is awaiting the
     * user's completion decision - never for a plain pause/break/reset (remaining > 0) or a
     * manual mark-done (status flips to COMPLETED), which share some of the same null/zero
     * fields but aren't a "just finished" event worth alerting about.
     */
    private fun isJustCompleted(task: TaskEntity): Boolean =
        task.timerMinutes != null &&
            task.timerEndAtMillis == null &&
            task.timerRemainingMillis == 0L &&
            task.status == TaskStatus.IN_PROGRESS

    private suspend fun runCountdown(task: TaskEntity) {
        val endAtMillis = task.timerEndAtMillis ?: return
        while (true) {
            val remainingMillis = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            postNotification(NOTIFICATION_ID_RUNNING, buildRunningNotification(task.name, formatRemaining(remainingMillis)))
            if (remainingMillis <= 0) {
                taskRepository.finishTimer(task)
                return
            }
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    private fun postCompletionNotification(task: TaskEntity) {
        val durationLabel = task.timerMinutes?.let { getString(R.string.timer_minutes_format, it) }.orEmpty()
        val notification = NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.focus_timer_notification_complete_title, task.name))
            .setContentText(getString(R.string.focus_timer_notification_complete_text, durationLabel))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(buildContentIntent())
            .build()
        postNotification(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun postNotification(id: Int, notification: Notification) {
        // startForeground()'s own notification (and updates to that same id while the
        // foreground service is active) are exempt from the POST_NOTIFICATIONS runtime check,
        // but guard anyway in case a particular OEM/OS combination disagrees.
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (_: SecurityException) {
            // No notification permission - the timer itself keeps running correctly regardless,
            // since it's driven by the stored end timestamp, not by this notification.
        }
    }

    private fun buildRunningNotification(taskName: String, remainingLabel: String): Notification {
        val displayName = taskName.ifBlank { getString(R.string.focus_timer_title) }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.focus_timer_notification_running_title))
            .setContentText(getString(R.string.focus_timer_notification_running_text, displayName, remainingLabel))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Explicit per-notification visibility: on API 26+ the CHANNEL's own lockscreen
            // visibility (set below) is what Android actually honors, but this also keeps any
            // legacy/OEM path that still reads the notification-level flag from defaulting to
            // VISIBILITY_PRIVATE, which some lock screens treat as "redact to a generic entry"
            // rather than "show it plainly" - there's no other task information here beyond the
            // task name and remaining time, so there's nothing further to redact.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(buildContentIntent())
            .build()
    }

    private fun buildContentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        // Drops the old pre-fix channel (a no-op if it was never created on this device) so a
        // device that already ran an earlier build doesn't end up with two identically-named
        // "Focus timer" entries under system notification settings.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)

        val runningChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.focus_timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            // A channel's settings are frozen the first time it's created on a given device -
            // recreating it with the same id later (e.g. a previous build's install) has no
            // effect, which is why CHANNEL_ID below was bumped alongside this fix: without a
            // fresh channel id, a device that already ran an earlier build would stay stuck
            // without lock-screen visibility no matter what this method sets from then on.
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(runningChannel)

        // A separate, normal-importance channel for the one-shot "session complete" alert - it's
        // a discrete event the user likely wants to actually notice (sound/visual), unlike the
        // silent, continuously-updating running-session notification above, which deliberately
        // stays low-importance so it doesn't re-alert on every tick.
        val completeChannel = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            getString(R.string.focus_timer_notification_complete_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(completeChannel)
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val NOTIFICATION_ID_RUNNING = 4201
        // Distinct from NOTIFICATION_ID_RUNNING so posting it never replaces/cancels the ongoing
        // running notification (and vice versa), and so it survives after the running one is
        // torn down when the foreground service stops.
        private const val NOTIFICATION_ID_COMPLETE = 4202
        // v2: the channel is now created with explicit lock-screen visibility - see
        // createNotificationChannel(). A new id forces every device (including ones that
        // already ran an earlier build with the un-fixed channel) to get these settings, since
        // a channel's own properties can't be changed once it exists.
        private const val CHANNEL_ID = "focus_timer_channel_v2"
        private const val COMPLETE_CHANNEL_ID = "focus_timer_complete_channel"
        private const val LEGACY_CHANNEL_ID = "focus_timer_channel"
        private const val TICK_INTERVAL_MILLIS = 1_000L

        fun start(context: Context, taskId: String) {
            val intent = Intent(context, TimerForegroundService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
