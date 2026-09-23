package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.timer.DEFAULT_TIMER_DURATION_MILLIS
import com.nj031.onetask.data.timer.FOCUS_BREAK_DURATION_MILLIS
import com.nj031.onetask.data.timer.FocusCallsMode
import com.nj031.onetask.data.timer.FocusNotificationsMode
import com.nj031.onetask.data.timer.FocusOverlayState
import com.nj031.onetask.data.timer.TimerMode
import com.nj031.onetask.data.timer.TimerSessionRepository
import com.nj031.onetask.data.timer.TimerSessionSnapshot
import com.nj031.onetask.service.FocusNotificationPolicyManager
import com.nj031.onetask.service.StandaloneTimerForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Backs the Timer screen (both its Timer and Stopwatch tabs). Owns no live-counting state of its
 * own beyond a UI-refresh tick - the single source of truth is TimerSessionRepository (survives
 * process death), independently kept moving in the background by StandaloneTimerForegroundService
 * whether or not this ViewModel/screen exists at all. [selectedIdleDurationMillis] is the one
 * piece of UI state that deliberately does NOT persist across process death or Stop - it's a
 * plain in-memory default (always 25 min on a fresh instance), matching the spec's "don't
 * remember the last preset" requirement for the idle (nothing started yet) case.
 *
 * Also owns [focusOverlay] - the Timer tab's distraction-blocking Focus Mode session (see
 * FocusOverlayState's own doc comment for why this rides on top of the plain Timer/Stopwatch
 * engine above rather than being a separate one). A Focus session can be riding on either
 * engine - its own pause/resume/stop always act on whichever one is currently active (see
 * TimerSessionSnapshot.activeMode), since the two are already mutually exclusive by
 * construction (see TimerSessionRepository). This is a different, unrelated system from the
 * existing task-based Focus Mode (FocusTimerScreen/TimerForegroundService/FocusSessionState),
 * which this class never touches.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimerSessionRepository(application)

    private val _snapshot = MutableStateFlow(repository.snapshot())
    val snapshot: StateFlow<TimerSessionSnapshot> = _snapshot.asStateFlow()

    private val _selectedIdleDurationMillis = MutableStateFlow(DEFAULT_TIMER_DURATION_MILLIS)
    val selectedIdleDurationMillis: StateFlow<Long> = _selectedIdleDurationMillis.asStateFlow()

    private val _focusOverlay = MutableStateFlow<FocusOverlayState?>(null)
    val focusOverlay: StateFlow<FocusOverlayState?> = _focusOverlay.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                var current = repository.snapshot()
                // finishTimerIfDue() is idempotent and safe to call alongside the foreground
                // service's own independent tick - whichever of the two observes the countdown
                // reach zero first does the (already-working) notification/completion side effects;
                // this call's only job is making sure the in-app snapshot flips back to idle
                // promptly even if the service's own tick hasn't run yet.
                if (current.activeMode == TimerMode.TIMER && repository.finishTimerIfDue()) {
                    current = repository.snapshot()
                }
                _snapshot.value = current

                val overlay = _focusOverlay.value
                if (overlay != null) {
                    if (current.activeMode == null) {
                        // The Focus session's own underlying Timer/Stopwatch ended (natural Timer
                        // completion just above, or an explicit stopTimer()/stopStopwatch()) - the
                        // overlay never outlives it: no break prompt, no history, straight back to
                        // the normal idle Timer/Stopwatch screen.
                        _focusOverlay.value = null
                        repository.clearFocusBlocking()
                        overlay.priorNotificationPolicy?.let {
                            FocusNotificationPolicyManager.restore(getApplication(), it)
                        }
                    } else if (overlay.breakEndAtMillis != null && System.currentTimeMillis() >= overlay.breakEndAtMillis) {
                        when (current.activeMode) {
                            TimerMode.TIMER -> resumeTimer()
                            TimerMode.STOPWATCH -> resumeStopwatch()
                            null -> {}
                        }
                        _focusOverlay.value = overlay.copy(breakEndAtMillis = null)
                        repository.setFocusBreakEndAtMillis(null)
                        FocusNotificationPolicyManager.applyPolicy(
                            getApplication(),
                            overlay.notificationsMode,
                            overlay.callsMode
                        )
                    }
                }

                delay(if (current.activeMode != null || _focusOverlay.value != null) 250L else 2_000L)
            }
        }
    }

    fun selectIdleDuration(durationMillis: Long) {
        _selectedIdleDurationMillis.value = durationMillis
    }

    fun startTimer() {
        repository.startTimer(_selectedIdleDurationMillis.value)
        _snapshot.value = repository.snapshot()
        StandaloneTimerForegroundService.start(getApplication())
    }

    fun pauseTimer() {
        repository.pauseTimer()
        _snapshot.value = repository.snapshot()
    }

    fun resumeTimer() {
        repository.resumeTimer()
        _snapshot.value = repository.snapshot()
    }

    fun stopTimer() {
        repository.stopTimer()
        _snapshot.value = repository.snapshot()
        _selectedIdleDurationMillis.value = DEFAULT_TIMER_DURATION_MILLIS
        StandaloneTimerForegroundService.stop(getApplication())
    }

    fun startStopwatch() {
        repository.startStopwatch()
        _snapshot.value = repository.snapshot()
        StandaloneTimerForegroundService.start(getApplication())
    }

    fun pauseStopwatch() {
        repository.pauseStopwatch()
        _snapshot.value = repository.snapshot()
    }

    fun resumeStopwatch() {
        repository.resumeStopwatch()
        _snapshot.value = repository.snapshot()
    }

    fun stopStopwatch() {
        repository.stopStopwatch()
        _snapshot.value = repository.snapshot()
        StandaloneTimerForegroundService.stop(getApplication())
    }

    /** Starts a Timer Focus session: an ordinary Timer countdown (same engine, same background
     * survival as a normal running Timer - see the class doc comment) plus the break bookkeeping
     * layered on top. Replaces whatever normal Timer/Stopwatch session (if any) was previously
     * active, exactly like starting a plain Timer already does. [blockedPackages] is the Block
     * Distractions selection (see FocusBlockedAppsViewModel.selectedPackages) as of this moment -
     * carried on the overlay for a later phase to enforce; this call only transports it. */
    fun startFocusSession(
        durationMillis: Long,
        breaksTotal: Int,
        blockedPackages: Set<String> = emptySet(),
        notificationsMode: FocusNotificationsMode = FocusNotificationsMode.ALLOW,
        callsMode: FocusCallsMode = FocusCallsMode.ALLOW,
        strictModeEnabled: Boolean = false
    ) {
        selectIdleDuration(durationMillis)
        startTimer()
        val priorNotificationPolicy = FocusNotificationPolicyManager.captureCurrentState(getApplication())
        _focusOverlay.value = FocusOverlayState(
            breaksTotal = breaksTotal,
            breaksRemaining = breaksTotal,
            blockedPackages = blockedPackages,
            notificationsMode = notificationsMode,
            callsMode = callsMode,
            priorNotificationPolicy = priorNotificationPolicy,
            strictModeEnabled = strictModeEnabled
        )
        repository.setFocusBlockedPackages(blockedPackages)
        FocusNotificationPolicyManager.applyPolicy(getApplication(), notificationsMode, callsMode)
    }

    /** Starts a Stopwatch Focus session: an ordinary Stopwatch (counts up from 00:00, no fixed
     * duration or completion point - see the class doc comment) plus the same break bookkeeping
     * startFocusSession layers onto a Timer. See [startFocusSession] for the shared params. */
    fun startStopwatchFocusSession(
        breaksTotal: Int,
        blockedPackages: Set<String> = emptySet(),
        notificationsMode: FocusNotificationsMode = FocusNotificationsMode.ALLOW,
        callsMode: FocusCallsMode = FocusCallsMode.ALLOW,
        strictModeEnabled: Boolean = false
    ) {
        startStopwatch()
        val priorNotificationPolicy = FocusNotificationPolicyManager.captureCurrentState(getApplication())
        _focusOverlay.value = FocusOverlayState(
            breaksTotal = breaksTotal,
            breaksRemaining = breaksTotal,
            blockedPackages = blockedPackages,
            notificationsMode = notificationsMode,
            callsMode = callsMode,
            priorNotificationPolicy = priorNotificationPolicy,
            strictModeEnabled = strictModeEnabled
        )
        repository.setFocusBlockedPackages(blockedPackages)
        FocusNotificationPolicyManager.applyPolicy(getApplication(), notificationsMode, callsMode)
    }

    /** Terminates the Focus session outright - not a completion, so nothing is recorded anywhere
     * (there is no Focus History in this phase). Stops whichever engine (Timer or Stopwatch) the
     * session is currently riding on. Callers must not reach this while
     * [FocusOverlayState.strictModeEnabled] is true and the session hasn't ended naturally - see
     * TimerPlaceholderScreen's gating of both UI paths that call this. */
    fun stopFocusSession() {
        val overlay = _focusOverlay.value
        when (_snapshot.value.activeMode) {
            TimerMode.TIMER -> stopTimer()
            TimerMode.STOPWATCH -> stopStopwatch()
            null -> {}
        }
        _focusOverlay.value = null
        repository.clearFocusBlocking()
        overlay?.priorNotificationPolicy?.let {
            FocusNotificationPolicyManager.restore(getApplication(), it)
        }
    }

    /** Consumes exactly one break (no-op, returns false, if none remain or one is already in
     * progress): freezes the Focus session's exact elapsed/remaining time via the existing
     * pauseTimer()/pauseStopwatch() (whichever engine is active), then starts a separate
     * 10-minute break countdown. The break auto-resumes the Focus session from that exact
     * preserved time when it reaches zero (see the init tick loop). */
    fun takeFocusBreak(): Boolean {
        val overlay = _focusOverlay.value ?: return false
        if (overlay.isOnBreak || overlay.breaksRemaining <= 0) return false
        when (_snapshot.value.activeMode) {
            TimerMode.TIMER -> pauseTimer()
            TimerMode.STOPWATCH -> pauseStopwatch()
            null -> return false
        }
        val breakEndAtMillis = System.currentTimeMillis() + FOCUS_BREAK_DURATION_MILLIS
        _focusOverlay.value = overlay.copy(
            breaksRemaining = overlay.breaksRemaining - 1,
            breakEndAtMillis = breakEndAtMillis
        )
        repository.setFocusBreakEndAtMillis(breakEndAtMillis)
        overlay.priorNotificationPolicy?.let {
            FocusNotificationPolicyManager.restore(getApplication(), it)
        }
        return true
    }

    /** Ends the current manual Focus break immediately, before its 10-minute countdown elapses -
     * resumes whichever engine (Timer or Stopwatch) the Focus session is riding on from its exact
     * frozen remaining/elapsed time (the same resumeTimer()/resumeStopwatch() calls the natural
     * break-timeout path in the init tick loop already uses), and clears breakEndAtMillis both
     * in-memory and in the durable mirror FocusBlockingAccessibilityService reads - blocking
     * resumes as soon as that clear is observed, with no further wiring needed here. Deliberately
     * never touches breaksRemaining: the break was already consumed the moment it started (see
     * takeFocusBreak above), so ending it early must not return it, and must not consume a second
     * one either. No-op if no break is currently in progress. */
    fun endFocusBreak() {
        val overlay = _focusOverlay.value ?: return
        if (!overlay.isOnBreak) return
        when (_snapshot.value.activeMode) {
            TimerMode.TIMER -> resumeTimer()
            TimerMode.STOPWATCH -> resumeStopwatch()
            null -> return
        }
        _focusOverlay.value = overlay.copy(breakEndAtMillis = null)
        repository.setFocusBreakEndAtMillis(null)
        FocusNotificationPolicyManager.applyPolicy(getApplication(), overlay.notificationsMode, overlay.callsMode)
    }
}
