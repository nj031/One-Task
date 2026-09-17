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
import com.nj031.onetask.data.settings.GeneralSettingsRepository
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
import kotlinx.coroutines.flow.first
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

    // Read once per service instance (a fresh one is created for every focus session start,
    // including a cold-start resume) - both default to true, matching this service's own
    // always-on behavior before these settings existed.
    private var notificationsEnabled = true
    private var completionNotificationEnabled = true

    // True once startForeground() has actually been called for this service instance. Guards
    // against re-posting the "--:--" placeholder (and the visible flicker that would cause) on
    // every Pause/Resume notification-action tap while the service is already alive and already
    // showing live content - startForeground() only needs to run once per instance; after that,
    // ordinary notify() calls (see postNotification) keep the same notification up to date.
    private var hasStartedForeground = false

    override fun onCreate() {
        super.onCreate()
        taskRepository = TaskRepository(AppDatabase.getInstance(applicationContext).taskDao())
        val settings = GeneralSettingsRepository(applicationContext)
        notificationsEnabled = settings.getFocusSessionNotificationsEnabled()
        completionNotificationEnabled = settings.getFocusSessionCompleteEnabled()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (taskId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasStartedForeground) {
            startForeground(
                NOTIFICATION_ID_RUNNING,
                buildRunningNotification(taskId = taskId, taskName = "", remainingLabel = "--:--", isPaused = false)
            )
            hasStartedForeground = true
        }

        // The notification's own Pause/Resume actions re-enter this same service via a fresh
        // onStartCommand call rather than a separate broadcast receiver - reusing the exact
        // TaskRepository methods (pauseTimer/startTimer) that FocusTimerScreen's in-app
        // Pause/Resume buttons already call, so the DB write - and everything reacting to it,
        // including this service's own observeTask below and Focus Mode's own live task
        // observation - is identical regardless of which one triggered it. The write is awaited
        // before (re)subscribing observeTask so its very first emission already reflects the new
        // state, rather than racing a stale "still running"/"still paused" snapshot against it.
        when (intent.action) {
            ACTION_PAUSE -> serviceScope.launch {
                applyPauseAction(taskId)
                observeTask(taskId)
            }
            ACTION_RESUME -> serviceScope.launch {
                applyResumeAction(taskId)
                observeTask(taskId)
            }
            else -> observeTask(taskId)
        }
        return START_NOT_STICKY
    }

    private suspend fun applyPauseAction(taskId: String) {
        val task = taskRepository.observeTaskById(taskId).first() ?: return
        if (task.timerEndAtMillis != null) {
            taskRepository.pauseTimer(task)
        }
    }

    private suspend fun applyResumeAction(taskId: String) {
        val task = taskRepository.observeTaskById(taskId).first() ?: return
        if (task.timerEndAtMillis == null) {
            taskRepository.startTimer(task)
        }
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
                if (task != null && isJustCompleted(task) && completionNotificationEnabled) {
                    postCompletionNotification(task)
                }
                when {
                    // Left the session entirely: task gone, marked Done, Reset back to Not
                    // Started, or (defensively) its timer was removed outright. Nothing left for
                    // this notification to show, so tear the whole thing down - unchanged from
                    // this service's original behavior.
                    task == null || task.status != TaskStatus.IN_PROGRESS || task.timerMinutes == null -> {
                        stopSelf()
                    }
                    // Actively counting down.
                    task.timerEndAtMillis != null -> {
                        runCountdown(task)
                    }
                    // Just reached zero naturally - isJustCompleted's own notification above
                    // takes over from here, so the running notification's job is done too.
                    isJustCompleted(task) -> {
                        stopSelf()
                    }
                    // A genuine mid-session pause (Pause/Break/Leave Focus, from either the
                    // notification's own Pause action or the in-app Focus Mode controls): keep
                    // this service and its notification alive - unlike every other case above -
                    // showing the frozen remaining time and a Resume action, so tapping Resume
                    // from the notification is possible at all.
                    else -> {
                        postPausedNotification(task)
                    }
                }
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

    private fun postPausedNotification(task: TaskEntity) {
        val remainingMillis = task.timerRemainingMillis ?: 0L
        val notification = buildRunningNotification(
            taskId = task.id,
            taskName = task.name,
            remainingLabel = formatRemaining(remainingMillis),
            isPaused = true
        )
        postNotification(NOTIFICATION_ID_RUNNING, notification)
    }

    private suspend fun runCountdown(task: TaskEntity) {
        val endAtMillis = task.timerEndAtMillis ?: return
        // Android requires a foreground service to keep a notification posted the whole time
        // it runs - that requirement is met unconditionally by startForeground() in
        // onStartCommand and is never skipped here. What IS optional, and what
        // notificationsEnabled actually gates, is the live per-second countdown text refresh:
        // when disabled, the required notification stays up but only gets a single static
        // update instead of ticking every second.
        var staticNotificationPosted = false
        while (true) {
            val remainingMillis = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            if (notificationsEnabled) {
                postNotification(
                    NOTIFICATION_ID_RUNNING,
                    buildRunningNotification(
                        taskId = task.id,
                        taskName = task.name,
                        remainingLabel = formatRemaining(remainingMillis),
                        isPaused = false
                    )
                )
            } else if (!staticNotificationPosted) {
                postNotification(
                    NOTIFICATION_ID_RUNNING,
                    buildRunningNotification(taskId = task.id, taskName = task.name, remainingLabel = null, isPaused = false)
                )
                staticNotificationPosted = true
            }
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
            // HIGH is required (not just requested) for this to pop up as a heads-up banner -
            // on API 26+ the channel's own importance below is what Android actually honors for
            // that, but this keeps any legacy/OEM path that still reads the notification-level
            // field from falling back to a silent, shade-only default.
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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

    /**
     * The running/paused notification's content: just the task name (title) and the remaining
     * time (text) - no repeated "One Task"/"Focus session running" chrome, since the system
     * template already shows the app name/icon/timestamp on its own. [isPaused] swaps the
     * Pause action for Resume; both states otherwise share the exact same compact 2-line layout.
     *
     * Typography: setContentTitle/setContentText render through Android's own standard
     * notification template, which does not support an app-supplied custom typeface here -
     * forcing one would require a fully custom RemoteViews layout, which this task explicitly
     * rules out. The system's native notification typography (and its own built-in title/body
     * hierarchy) is used as-is, per Android's supported notification layout behavior.
     */
    private fun buildRunningNotification(
        taskId: String,
        taskName: String,
        remainingLabel: String?,
        isPaused: Boolean
    ): Notification {
        val displayName = taskName.ifBlank { getString(R.string.focus_timer_title) }
        val contentText = if (remainingLabel != null) {
            getString(R.string.focus_timer_notification_remaining_format, remainingLabel)
        } else {
            getString(R.string.focus_timer_notification_running_minimal_text)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(displayName)
            .setContentText(contentText)
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
            .setContentIntent(buildOpenFocusModePendingIntent(taskId))
            .addAction(buildPauseOrResumeAction(taskId, isPaused))
            .addAction(buildOpenAction(taskId))
            .build()
    }

    private fun buildPauseOrResumeAction(taskId: String, isPaused: Boolean): NotificationCompat.Action {
        val action = Intent(this, TimerForegroundService::class.java).apply {
            this.action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        // getForegroundService (not the plain getService) is the API Android's docs call for a
        // notification action that must reliably start/re-enter a foreground service - it's
        // exempt from the background-service-start restrictions a bare startService() would hit
        // if this service's process had died while the notification was still showing.
        val pendingIntent = PendingIntent.getForegroundService(
            this,
            if (isPaused) REQUEST_CODE_RESUME else REQUEST_CODE_PAUSE,
            action,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon = if (isPaused) R.drawable.ic_notification_resume else R.drawable.ic_notification_pause
        val label = getString(if (isPaused) R.string.resume else R.string.pause)
        return NotificationCompat.Action.Builder(icon, label, pendingIntent).build()
    }

    private fun buildOpenAction(taskId: String): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification_open,
            getString(R.string.focus_timer_notification_open_action),
            buildOpenFocusModePendingIntent(taskId)
        ).build()
    }

    /**
     * Opens One Task straight into this task's Focus Mode, whatever state the app was in: a
     * cold start already resolves this via FocusSessionState (see MainActivity), while a warm
     * relaunch - the app process still alive, just not showing Focus Mode right now - is
     * handled by MainActivity.onNewIntent picking up EXTRA_OPEN_FOCUS_TASK_ID and navigating
     * there explicitly. Either way it lands on the exact same task/timer state this notification
     * itself reflects, since both read the same Room row.
     */
    private fun buildOpenFocusModePendingIntent(taskId: String): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_CODE_OPEN_FOCUS,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_FOCUS_TASK_ID, taskId)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Used only by the separate Timer Completion notification - unchanged by this task. */
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
        // Same reasoning as LEGACY_CHANNEL_ID above: the pre-fix complete channel was created
        // with IMPORTANCE_DEFAULT, which never shows a heads-up banner no matter what this
        // method sets on it afterward - only a fresh channel id (COMPLETE_CHANNEL_ID's own v2
        // bump below) actually picks up IMPORTANCE_HIGH on a device that already has the old one.
        manager.deleteNotificationChannel(LEGACY_COMPLETE_CHANNEL_ID)

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

        // A separate, high-importance channel for the one-shot "session complete" alert - it's a
        // discrete event the user likely wants to actually notice (heads-up popup + sound),
        // unlike the silent, continuously-updating running-session notification above, which
        // deliberately stays low-importance so it doesn't re-alert on every tick. HIGH (not
        // DEFAULT) is required for Android to show it as a heads-up banner rather than just
        // silently adding it to the shade.
        val completeChannel = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            getString(R.string.focus_timer_notification_complete_channel_name),
            NotificationManager.IMPORTANCE_HIGH
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
        // v2: bumped from IMPORTANCE_DEFAULT to IMPORTANCE_HIGH so the completion alert actually
        // shows as a heads-up banner - see createNotificationChannel(). Same "properties are
        // frozen at creation" reasoning as CHANNEL_ID's own v2 bump above.
        private const val COMPLETE_CHANNEL_ID = "focus_timer_complete_channel_v2"
        private const val LEGACY_CHANNEL_ID = "focus_timer_channel"
        private const val LEGACY_COMPLETE_CHANNEL_ID = "focus_timer_complete_channel"
        private const val TICK_INTERVAL_MILLIS = 1_000L

        private const val ACTION_PAUSE = "com.nj031.onetask.action.PAUSE_TIMER"
        private const val ACTION_RESUME = "com.nj031.onetask.action.RESUME_TIMER"
        // Distinct request codes per PendingIntent so Android never collapses/reuses one
        // action's extras for another - all three can be simultaneously "in flight" (posted on
        // the current notification) at once.
        private const val REQUEST_CODE_OPEN_FOCUS = 1
        private const val REQUEST_CODE_PAUSE = 2
        private const val REQUEST_CODE_RESUME = 3

        fun start(context: Context, taskId: String) {
            val intent = Intent(context, TimerForegroundService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
