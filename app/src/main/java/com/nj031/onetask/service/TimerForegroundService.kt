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

        startForeground(NOTIFICATION_ID, buildNotification(taskName = "", remainingLabel = "--:--"))
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
                if (task == null || task.status != TaskStatus.IN_PROGRESS || task.timerEndAtMillis == null) {
                    stopSelf()
                    return@collectLatest
                }
                runCountdown(task)
            }
        }
    }

    private suspend fun runCountdown(task: TaskEntity) {
        val endAtMillis = task.timerEndAtMillis ?: return
        while (true) {
            val remainingMillis = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            postNotification(buildNotification(task.name, formatRemaining(remainingMillis)))
            if (remainingMillis <= 0) {
                taskRepository.finishTimer(task)
                return
            }
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    private fun postNotification(notification: Notification) {
        // startForeground()'s own notification (and updates to that same id while the
        // foreground service is active) are exempt from the POST_NOTIFICATIONS runtime check,
        // but guard anyway in case a particular OEM/OS combination disagrees.
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // No notification permission - the timer itself keeps running correctly regardless,
            // since it's driven by the stored end timestamp, not by this notification.
        }
    }

    private fun buildNotification(taskName: String, remainingLabel: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(taskName.ifBlank { getString(R.string.focus_timer_title) })
            .setContentText(getString(R.string.focus_timer_notification_text, remainingLabel))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.focus_timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
        private const val NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "focus_timer_channel"
        private const val TICK_INTERVAL_MILLIS = 1_000L

        fun start(context: Context, taskId: String) {
            val intent = Intent(context, TimerForegroundService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
