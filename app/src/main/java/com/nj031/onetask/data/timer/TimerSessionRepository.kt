package com.nj031.onetask.data.timer

import android.content.Context

enum class TimerMode { TIMER, STOPWATCH }

/** 25 minutes - the Timer's default duration whenever nothing is running/paused, matching the
 * default shown on a fresh Timer screen (also the value Stop always returns to). */
const val DEFAULT_TIMER_DURATION_MILLIS = 25 * 60_000L

/** Minimum duration a Custom timer may be set to - the picker itself never allows less. */
const val MIN_TIMER_DURATION_MILLIS = 5_000L

data class TimerSessionSnapshot(
    val activeMode: TimerMode?,
    /** Non-null only while a Timer session is actively counting down (absolute end timestamp). */
    val timerEndAtMillis: Long?,
    /** Non-null only while a Timer session is paused (frozen remaining duration). */
    val timerRemainingMillis: Long?,
    /** The duration this Timer session was started with - used for the ring's "total" reference
     * and to redisplay the same value across pause/resume/process death. Meaningless while idle. */
    val timerTotalDurationMillis: Long,
    /** Non-null only while a Stopwatch session is actively running (absolute start timestamp of
     * the current running segment). */
    val stopwatchStartedAtMillis: Long?,
    /** The frozen elapsed total from every already-completed running segment - 0 while idle,
     * unchanged while running (the live segment is added on top of this), frozen while paused. */
    val stopwatchAccumulatedMillis: Long
) {
    val isTimerRunning: Boolean get() = activeMode == TimerMode.TIMER && timerEndAtMillis != null
    val isTimerPaused: Boolean get() = activeMode == TimerMode.TIMER && timerEndAtMillis == null
    val isStopwatchRunning: Boolean get() = activeMode == TimerMode.STOPWATCH && stopwatchStartedAtMillis != null
    val isStopwatchPaused: Boolean get() = activeMode == TimerMode.STOPWATCH && stopwatchStartedAtMillis == null

    /** Current remaining Timer duration, recomputed from the live end timestamp rather than
     * assumed from any decrementing counter - correct whether it's been 1 second or 1 hour since
     * this snapshot was taken. */
    fun timerRemainingNowMillis(): Long = when {
        timerEndAtMillis != null -> (timerEndAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        timerRemainingMillis != null -> timerRemainingMillis
        else -> timerTotalDurationMillis
    }

    /** Current elapsed Stopwatch duration, likewise recomputed from the live start timestamp. */
    fun stopwatchElapsedNowMillis(): Long = when {
        stopwatchStartedAtMillis != null -> stopwatchAccumulatedMillis + (System.currentTimeMillis() - stopwatchStartedAtMillis)
        else -> stopwatchAccumulatedMillis
    }
}

/**
 * Persists the Timer/Stopwatch feature's active session, the same SharedPreferences-backed
 * pattern GeneralSettingsRepository already uses instead of a new Room table: this is a handful
 * of simple values, and AppDatabase's fallbackToDestructiveMigration() on any version bump makes
 * a new Room table not worth risking every existing install's Tasks/Notes data over.
 *
 * Only an actively running or paused session is ever persisted here - the idle state (nothing
 * started yet) is represented by simply having no keys stored at all, which is also exactly what
 * a fresh install reads. Timer and Stopwatch are mutually exclusive: starting one always implies
 * the other is/stays idle (enforced by the UI layer disabling the inactive tab, not by this
 * repository, which only ever writes one mode's fields at a time).
 *
 * Every mutation here recomputes "now" itself rather than trusting any previously-read value, and
 * every read (timerRemainingNowMillis/stopwatchElapsedNowMillis) is similarly derived from an
 * absolute timestamp - never from an in-memory decrementing counter - so a running/paused session
 * recovers its exact correct state whether the process was merely backgrounded or fully killed
 * and restarted.
 */
class TimerSessionRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): TimerSessionSnapshot {
        val mode = prefs.getString(KEY_ACTIVE_MODE, null)
            ?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() }
        return TimerSessionSnapshot(
            activeMode = mode,
            timerEndAtMillis = prefs.getLong(KEY_TIMER_END_AT, -1L).takeIf { it >= 0L },
            timerRemainingMillis = prefs.getLong(KEY_TIMER_REMAINING, -1L).takeIf { it >= 0L },
            timerTotalDurationMillis = prefs.getLong(KEY_TIMER_TOTAL, DEFAULT_TIMER_DURATION_MILLIS),
            stopwatchStartedAtMillis = prefs.getLong(KEY_STOPWATCH_STARTED_AT, -1L).takeIf { it >= 0L },
            stopwatchAccumulatedMillis = prefs.getLong(KEY_STOPWATCH_ACCUMULATED, 0L)
        )
    }

    fun startTimer(durationMillis: Long) {
        prefs.edit()
            .putString(KEY_ACTIVE_MODE, TimerMode.TIMER.name)
            .putLong(KEY_TIMER_END_AT, System.currentTimeMillis() + durationMillis)
            .remove(KEY_TIMER_REMAINING)
            .putLong(KEY_TIMER_TOTAL, durationMillis)
            .apply()
    }

    fun pauseTimer() {
        val snap = snapshot()
        if (snap.activeMode != TimerMode.TIMER || snap.timerEndAtMillis == null) return
        prefs.edit()
            .remove(KEY_TIMER_END_AT)
            .putLong(KEY_TIMER_REMAINING, snap.timerRemainingNowMillis())
            .apply()
    }

    fun resumeTimer() {
        val snap = snapshot()
        val remaining = snap.timerRemainingMillis ?: return
        if (snap.activeMode != TimerMode.TIMER) return
        prefs.edit()
            .putLong(KEY_TIMER_END_AT, System.currentTimeMillis() + remaining)
            .remove(KEY_TIMER_REMAINING)
            .apply()
    }

    /** Fully clears the Timer session, returning to idle - used both by an explicit user Stop and
     * internally by [finishTimerIfDue] once the countdown has genuinely reached zero. */
    fun stopTimer() {
        prefs.edit()
            .remove(KEY_ACTIVE_MODE)
            .remove(KEY_TIMER_END_AT)
            .remove(KEY_TIMER_REMAINING)
            .remove(KEY_TIMER_TOTAL)
            .apply()
    }

    /** Clears the Timer session and returns true only the one time it actually detects the
     * countdown has reached zero while still active - a no-op (returns false) if it's already
     * idle/paused/mid-countdown, safe to call repeatedly from both the UI's own tick and the
     * foreground service's independent tick without double-firing completion effects. */
    fun finishTimerIfDue(): Boolean {
        val snap = snapshot()
        val endAt = snap.timerEndAtMillis ?: return false
        if (snap.activeMode != TimerMode.TIMER || System.currentTimeMillis() < endAt) return false
        stopTimer()
        return true
    }

    fun startStopwatch() {
        prefs.edit()
            .putString(KEY_ACTIVE_MODE, TimerMode.STOPWATCH.name)
            .putLong(KEY_STOPWATCH_STARTED_AT, System.currentTimeMillis())
            .putLong(KEY_STOPWATCH_ACCUMULATED, 0L)
            .apply()
    }

    fun pauseStopwatch() {
        val snap = snapshot()
        if (snap.activeMode != TimerMode.STOPWATCH || snap.stopwatchStartedAtMillis == null) return
        prefs.edit()
            .putLong(KEY_STOPWATCH_ACCUMULATED, snap.stopwatchElapsedNowMillis())
            .remove(KEY_STOPWATCH_STARTED_AT)
            .apply()
    }

    fun resumeStopwatch() {
        val snap = snapshot()
        if (snap.activeMode != TimerMode.STOPWATCH || snap.stopwatchStartedAtMillis != null) return
        prefs.edit()
            .putLong(KEY_STOPWATCH_STARTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun stopStopwatch() {
        prefs.edit()
            .remove(KEY_ACTIVE_MODE)
            .remove(KEY_STOPWATCH_STARTED_AT)
            .remove(KEY_STOPWATCH_ACCUMULATED)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "timer_session_prefs"
        private const val KEY_ACTIVE_MODE = "active_mode"
        private const val KEY_TIMER_END_AT = "timer_end_at_millis"
        private const val KEY_TIMER_REMAINING = "timer_remaining_millis"
        private const val KEY_TIMER_TOTAL = "timer_total_duration_millis"
        private const val KEY_STOPWATCH_STARTED_AT = "stopwatch_started_at_millis"
        private const val KEY_STOPWATCH_ACCUMULATED = "stopwatch_accumulated_millis"
    }
}

/** "H:MM:SS" once the duration reaches an hour, else "MM:SS" - shared by the notification and the
 * in-screen countdown/elapsed display so both always agree on formatting. */
fun formatTimerDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
