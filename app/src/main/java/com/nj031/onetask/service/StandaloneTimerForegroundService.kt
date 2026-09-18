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
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.timer.TimerMode
import com.nj031.onetask.data.timer.TimerSessionRepository
import com.nj031.onetask.data.timer.TimerSessionSnapshot
import com.nj031.onetask.data.timer.formatTimerDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns a running standalone Timer or Stopwatch session (Notes/Tasks' Focus Timer has its own,
 * separate TimerForegroundService - this is the Timer tab's equivalent) as a foreground service,
 * so it keeps counting accurately - and shows a persistent notification - while the app is
 * backgrounded, the screen is locked, or the app process is killed outright.
 *
 * Like TimerForegroundService, this service holds no session state of its own beyond what it
 * reads fresh from TimerSessionRepository (SharedPreferences here, rather than a Room row, since
 * there's no per-task entity to attach it to) on every tick - remaining/elapsed time is always
 * recomputed from an absolute timestamp, never assumed from a decrementing counter. Pause/Resume
 * notification actions re-enter this same service via a fresh onStartCommand (rather than a
 * broadcast receiver) and mutate the exact same repository the in-app UI's own Pause/Resume
 * buttons call, so both paths converge on identical state.
 */
class StandaloneTimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private lateinit var repository: TimerSessionRepository

    private var notificationsEnabled = true
    private var completionNotificationEnabled = true
    private var hasStartedForeground = false

    override fun onCreate() {
        super.onCreate()
        repository = TimerSessionRepository(applicationContext)
        val settings = GeneralSettingsRepository(applicationContext)
        notificationsEnabled = settings.getFocusSessionNotificationsEnabled()
        completionNotificationEnabled = settings.getFocusSessionCompleteEnabled()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasStartedForeground) {
            startForeground(NOTIFICATION_ID_RUNNING, buildRunningNotification(repository.snapshot()))
            hasStartedForeground = true
        }
        when (intent?.action) {
            ACTION_PAUSE -> pauseActiveSession()
            ACTION_RESUME -> resumeActiveSession()
        }
        ensureTickLoopRunning()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_RUNNING)
        super.onDestroy()
    }

    private fun pauseActiveSession() {
        when (repository.snapshot().activeMode) {
            TimerMode.TIMER -> repository.pauseTimer()
            TimerMode.STOPWATCH -> repository.pauseStopwatch()
            null -> {}
        }
    }

    private fun resumeActiveSession() {
        when (repository.snapshot().activeMode) {
            TimerMode.TIMER -> repository.resumeTimer()
            TimerMode.STOPWATCH -> repository.resumeStopwatch()
            null -> {}
        }
    }

    private fun ensureTickLoopRunning() {
        if (tickJob?.isActive == true) return
        tickJob = serviceScope.launch {
            while (isActive) {
                val snapshot = repository.snapshot()
                if (snapshot.activeMode == null) {
                    stopSelf()
                    return@launch
                }
                if (snapshot.activeMode == TimerMode.TIMER && repository.finishTimerIfDue()) {
                    if (completionNotificationEnabled) postCompletionNotification()
                    stopSelf()
                    return@launch
                }
                if (notificationsEnabled) {
                    postNotification(NOTIFICATION_ID_RUNNING, buildRunningNotification(repository.snapshot()))
                }
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private fun postNotification(id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (_: SecurityException) {
            // No notification permission - the session itself keeps running correctly regardless,
            // since it's driven by the stored end/start timestamp, not by this notification.
        }
    }

    private fun buildRunningNotification(snapshot: TimerSessionSnapshot): Notification {
        val isPaused = snapshot.isTimerPaused || snapshot.isStopwatchPaused
        val timeLabel = when (snapshot.activeMode) {
            TimerMode.TIMER -> getString(
                R.string.timer_notification_remaining_format,
                formatTimerDuration(snapshot.timerRemainingNowMillis())
            )
            TimerMode.STOPWATCH -> getString(
                R.string.timer_notification_elapsed_format,
                formatTimerDuration(snapshot.stopwatchElapsedNowMillis())
            )
            null -> ""
        }
        val contentText = if (isPaused) {
            getString(R.string.timer_notification_paused_prefix, timeLabel)
        } else {
            timeLabel
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(getString(R.string.timer_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(buildOpenPendingIntent())
            .addAction(buildPauseOrResumeAction(isPaused))
            .addAction(buildOpenAction())
            .build()
    }

    private fun postCompletionNotification() {
        val notification = NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.timer_notification_complete_title))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(buildOpenPendingIntent())
            .build()
        postNotification(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun buildPauseOrResumeAction(isPaused: Boolean): NotificationCompat.Action {
        val action = Intent(this, StandaloneTimerForegroundService::class.java).apply {
            this.action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
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

    private fun buildOpenAction(): NotificationCompat.Action = NotificationCompat.Action.Builder(
        R.drawable.ic_notification_open,
        getString(R.string.focus_timer_notification_open_action),
        buildOpenPendingIntent()
    ).build()

    private fun buildOpenPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_CODE_OPEN,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val runningChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(runningChannel)

        val completeChannel = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            getString(R.string.timer_notification_complete_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(completeChannel)
    }

    companion object {
        private const val NOTIFICATION_ID_RUNNING = 4301
        private const val NOTIFICATION_ID_COMPLETE = 4302
        private const val CHANNEL_ID = "standalone_timer_channel_v1"
        private const val COMPLETE_CHANNEL_ID = "standalone_timer_complete_channel_v1"
        private const val TICK_INTERVAL_MILLIS = 1_000L

        private const val ACTION_PAUSE = "com.nj031.onetask.action.PAUSE_STANDALONE_TIMER"
        private const val ACTION_RESUME = "com.nj031.onetask.action.RESUME_STANDALONE_TIMER"
        private const val REQUEST_CODE_OPEN = 1
        private const val REQUEST_CODE_PAUSE = 2
        private const val REQUEST_CODE_RESUME = 3

        /** Starts (or, if already running, simply ensures) the service - safe to call whenever a
         * Timer/Stopwatch session becomes active; a no-op in effect if it's already alive, since
         * onStartCommand's own tick loop only starts once. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, StandaloneTimerForegroundService::class.java))
        }

        /** Stops the service immediately - called right after an explicit in-app Stop so the
         * notification disappears without waiting for the tick loop's own idle self-stop check. */
        fun stop(context: Context) {
            context.stopService(Intent(context, StandaloneTimerForegroundService::class.java))
        }
    }
}
