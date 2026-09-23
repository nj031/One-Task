package com.nj031.onetask.data.timer

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.nj031.onetask.data.UserScopedPreferences

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
    val stopwatchAccumulatedMillis: Long,
    /** Wall-clock time this snapshot was taken. While a session is running, every other field
     * above is a static absolute timestamp/duration that does NOT itself change second to second
     * (the live remaining/elapsed value is only ever computed on demand below) - so without this
     * field, two snapshots read a second apart would be structurally equal, and MutableStateFlow's
     * built-in conflation (it never re-emits a value that equals() the previous one) would silently
     * drop every "still counting down/up" update, freezing the in-app UI while the independently
     * driven notification kept updating correctly. This field exists purely to make every snapshot
     * distinct so the UI actually recomposes each tick; it has no bearing on any state semantics. */
    val capturedAtMillis: Long = System.currentTimeMillis()
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

/** The Focus-blocking state [TimerSessionRepository.focusBlockingSnapshot] mirrors into durable
 * storage - see that method's own doc comment for why this mirror exists at all. */
data class FocusBlockingSnapshot(
    /** Real Android package names to block while a Focus session is active and not on break -
     * empty means no enforcement, whether because no Focus session is running at all or because
     * one is running with zero apps selected. */
    val blockedPackages: Set<String>,
    /** Non-null only while a manual Focus break is in progress (mirrors
     * FocusOverlayState.breakEndAtMillis exactly - same absolute end timestamp, not a separately
     * computed one, so the two never drift apart). */
    val breakEndAtMillis: Long?
) {
    val isOnBreak: Boolean get() = breakEndAtMillis != null
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
 * and restarted. The file itself is scoped per signed-in account (see [UserScopedPreferences]) -
 * a plain fixed file name would mean every account on this device shared the same running timer.
 *
 * Also mirrors a small slice of Focus-specific state (see [focusBlockingSnapshot] and its writer
 * methods below) for [com.nj031.onetask.service.FocusBlockingAccessibilityService] to read: that
 * service is a separate Android framework component, independent of whether this app's
 * Activity/ViewModel is currently alive, so the UI-scoped FocusOverlayState it would otherwise
 * read from TimerViewModel is not durable enough - see FocusOverlayState's own doc comment on why
 * it deliberately does NOT survive process death. Written to this same file/class specifically so
 * it inherits the exact process-death survivability every other field here already has, rather
 * than introducing a second persistence mechanism.
 */
class TimerSessionRepository(context: Context) {
    private val prefs = UserScopedPreferences.open(context, PREFS_NAME)

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

    /** Reads back the durable Focus-blocking mirror - see [FocusBlockingSnapshot] and this
     * class's own doc comment for why this exists. Never throws/crashes on a missing/empty value:
     * a fresh install or an account with no Focus history simply reads as "no enforcement." */
    fun focusBlockingSnapshot(): FocusBlockingSnapshot = FocusBlockingSnapshot(
        blockedPackages = prefs.getStringSet(KEY_FOCUS_BLOCKED_PACKAGES, emptySet()) ?: emptySet(),
        breakEndAtMillis = prefs.getLong(KEY_FOCUS_BREAK_END_AT, -1L).takeIf { it >= 0L }
    )

    /** Called by TimerViewModel at Focus session start, alongside setting the same value on the
     * in-memory FocusOverlayState.blockedPackages - see that field's own doc comment. Passing an
     * empty set (no apps selected) is the normal, expected way enforcement stays off for a Focus
     * session that has nothing to block. */
    fun setFocusBlockedPackages(packages: Set<String>) {
        // SharedPreferences.putStringSet stores by reference if not copied - passing a fresh
        // HashSet avoids the documented footgun of a caller later mutating the same Set instance
        // still referenced by a getStringSet() call made before this write.
        prefs.edit().putStringSet(KEY_FOCUS_BLOCKED_PACKAGES, HashSet(packages)).apply()
    }

    /** Called by TimerViewModel alongside every FocusOverlayState.breakEndAtMillis mutation
     * (manual break start, break auto-resume) - [endAtMillis] must be the exact same absolute
     * timestamp already computed there (or null to clear), never independently recomputed, so the
     * in-memory overlay and this durable mirror can never drift apart. [breaksRemaining], when
     * given, updates the durable breaks-remaining count in the same write - only takeFocusBreak()
     * actually changes it, so every other caller omits it and leaves the persisted value alone. */
    fun setFocusBreakEndAtMillis(endAtMillis: Long?, breaksRemaining: Int? = null) {
        val editor = prefs.edit()
        if (endAtMillis != null) {
            editor.putLong(KEY_FOCUS_BREAK_END_AT, endAtMillis)
        } else {
            editor.remove(KEY_FOCUS_BREAK_END_AT)
        }
        if (breaksRemaining != null) {
            editor.putInt(KEY_FOCUS_BREAKS_REMAINING, breaksRemaining)
        }
        editor.apply()
    }

    /** Called once by TimerViewModel at Focus session start (alongside [setFocusBlockedPackages]),
     * persisting the rest of FocusOverlayState's fields - everything [restoreFocusOverlayState]
     * needs to reconstruct an equivalent instance after TimerViewModel is recreated (app
     * backgrounded/killed and reopened) while the session is still active. These fields are all
     * set once at start and never mutated afterward on the in-memory overlay either (only
     * breaksRemaining/breakEndAtMillis change over a session's lifetime - see
     * [setFocusBreakEndAtMillis]), so a single write here is enough. [breaksRemaining] starts
     * equal to [breaksTotal] - no break has been taken yet. */
    fun setFocusSessionConfig(
        breaksTotal: Int,
        notificationsMode: FocusNotificationsMode,
        callsMode: FocusCallsMode,
        strictModeEnabled: Boolean,
        priorNotificationPolicy: FocusNotificationPolicySnapshot
    ) {
        prefs.edit()
            .putBoolean(KEY_FOCUS_ACTIVE, true)
            .putInt(KEY_FOCUS_BREAKS_TOTAL, breaksTotal)
            .putInt(KEY_FOCUS_BREAKS_REMAINING, breaksTotal)
            .putString(KEY_FOCUS_NOTIFICATIONS_MODE, notificationsMode.name)
            .putString(KEY_FOCUS_CALLS_MODE, callsMode.name)
            .putBoolean(KEY_FOCUS_STRICT_MODE_ENABLED, strictModeEnabled)
            .putInt(KEY_FOCUS_PRIOR_INTERRUPTION_FILTER, priorNotificationPolicy.interruptionFilter)
            .putInt(KEY_FOCUS_PRIOR_RINGER_MODE, priorNotificationPolicy.ringerMode)
            .apply()
    }

    /** Reconstructs a FocusOverlayState equivalent to the one TimerViewModel lost when it was
     * recreated (app backgrounded/killed and reopened, or any other ViewModel recreation) while a
     * Focus session was still active - see TimerViewModel.init, the only caller. Returns null
     * whenever no Focus session is actually active, which [KEY_FOCUS_ACTIVE] - set only by
     * [setFocusSessionConfig], cleared only by [clearFocusBlocking] - tracks explicitly rather
     * than being inferred from [KEY_FOCUS_BLOCKED_PACKAGES] being non-empty: a Focus session with
     * zero blocked apps selected is completely normal and must not read back as "no session".
     *
     * Defensively clears the mirror and returns null if the underlying Timer/Stopwatch session has
     * already ended (activeMode == null) despite the Focus flag still being set - this should never
     * happen given every path that ends a Focus session already calls [clearFocusBlocking], but
     * never resurrects a phantom overlay for a session that no longer exists either way. */
    fun restoreFocusOverlayState(): FocusOverlayState? {
        if (!prefs.getBoolean(KEY_FOCUS_ACTIVE, false)) return null
        if (snapshot().activeMode == null) {
            clearFocusBlocking()
            return null
        }
        val notificationsMode = prefs.getString(KEY_FOCUS_NOTIFICATIONS_MODE, null)
            ?.let { runCatching { FocusNotificationsMode.valueOf(it) }.getOrNull() }
            ?: FocusNotificationsMode.ALLOW
        val callsMode = prefs.getString(KEY_FOCUS_CALLS_MODE, null)
            ?.let { runCatching { FocusCallsMode.valueOf(it) }.getOrNull() }
            ?: FocusCallsMode.ALLOW
        return FocusOverlayState(
            breaksTotal = prefs.getInt(KEY_FOCUS_BREAKS_TOTAL, 0),
            breaksRemaining = prefs.getInt(KEY_FOCUS_BREAKS_REMAINING, 0),
            breakEndAtMillis = prefs.getLong(KEY_FOCUS_BREAK_END_AT, -1L).takeIf { it >= 0L },
            blockedPackages = prefs.getStringSet(KEY_FOCUS_BLOCKED_PACKAGES, emptySet()) ?: emptySet(),
            notificationsMode = notificationsMode,
            callsMode = callsMode,
            priorNotificationPolicy = FocusNotificationPolicySnapshot(
                interruptionFilter = prefs.getInt(KEY_FOCUS_PRIOR_INTERRUPTION_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL),
                ringerMode = prefs.getInt(KEY_FOCUS_PRIOR_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
            ),
            strictModeEnabled = prefs.getBoolean(KEY_FOCUS_STRICT_MODE_ENABLED, false)
        )
    }

    /** Clears the entire Focus-blocking mirror - called by TimerViewModel alongside every path
     * that sets FocusOverlayState back to null (explicit Focus stop, natural Timer completion). */
    fun clearFocusBlocking() {
        prefs.edit()
            .remove(KEY_FOCUS_BLOCKED_PACKAGES)
            .remove(KEY_FOCUS_BREAK_END_AT)
            .remove(KEY_FOCUS_ACTIVE)
            .remove(KEY_FOCUS_BREAKS_TOTAL)
            .remove(KEY_FOCUS_BREAKS_REMAINING)
            .remove(KEY_FOCUS_NOTIFICATIONS_MODE)
            .remove(KEY_FOCUS_CALLS_MODE)
            .remove(KEY_FOCUS_STRICT_MODE_ENABLED)
            .remove(KEY_FOCUS_PRIOR_INTERRUPTION_FILTER)
            .remove(KEY_FOCUS_PRIOR_RINGER_MODE)
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
        private const val KEY_FOCUS_BLOCKED_PACKAGES = "focus_blocked_packages"
        private const val KEY_FOCUS_BREAK_END_AT = "focus_break_end_at_millis"
        private const val KEY_FOCUS_ACTIVE = "focus_active"
        private const val KEY_FOCUS_BREAKS_TOTAL = "focus_breaks_total"
        private const val KEY_FOCUS_BREAKS_REMAINING = "focus_breaks_remaining"
        private const val KEY_FOCUS_NOTIFICATIONS_MODE = "focus_notifications_mode"
        private const val KEY_FOCUS_CALLS_MODE = "focus_calls_mode"
        private const val KEY_FOCUS_STRICT_MODE_ENABLED = "focus_strict_mode_enabled"
        private const val KEY_FOCUS_PRIOR_INTERRUPTION_FILTER = "focus_prior_interruption_filter"
        private const val KEY_FOCUS_PRIOR_RINGER_MODE = "focus_prior_ringer_mode"
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
