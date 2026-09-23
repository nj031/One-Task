package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.timer.DEFAULT_TIMER_DURATION_MILLIS
import com.nj031.onetask.data.timer.FOCUS_BREAK_DURATION_MILLIS
import com.nj031.onetask.data.timer.FocusOverlayState
import com.nj031.onetask.data.timer.TimerMode
import com.nj031.onetask.data.timer.TimerSessionRepository
import com.nj031.onetask.data.timer.TimerSessionSnapshot
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
 * FocusOverlayState's own doc comment for why this rides on top of the plain Timer countdown
 * above rather than being a separate engine). This is a different, unrelated system from the
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
                    if (current.activeMode != TimerMode.TIMER) {
                        // The Focus session's own underlying Timer ended (natural completion just
                        // above, or an explicit stopTimer()) - the overlay never outlives it: no
                        // break prompt, no history, straight back to the normal idle Timer screen.
                        _focusOverlay.value = null
                    } else if (overlay.breakEndAtMillis != null && System.currentTimeMillis() >= overlay.breakEndAtMillis) {
                        resumeTimer()
                        _focusOverlay.value = overlay.copy(breakEndAtMillis = null)
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

    /** Starts a Focus session: an ordinary Timer countdown (same engine, same background
     * survival as a normal running Timer - see the class doc comment) plus the break bookkeeping
     * layered on top. Replaces whatever normal Timer/Stopwatch session (if any) was previously
     * active, exactly like starting a plain Timer already does. */
    fun startFocusSession(durationMillis: Long, breaksTotal: Int) {
        selectIdleDuration(durationMillis)
        startTimer()
        _focusOverlay.value = FocusOverlayState(breaksTotal = breaksTotal, breaksRemaining = breaksTotal)
    }

    /** Terminates the Focus session outright - not a completion, so nothing is recorded anywhere
     * (there is no Focus History in this phase). Reuses stopTimer() as-is. */
    fun stopFocusSession() {
        stopTimer()
        _focusOverlay.value = null
    }

    /** Consumes exactly one break (no-op, returns false, if none remain or one is already in
     * progress): freezes the Focus Timer's exact remaining time via the existing pauseTimer(),
     * then starts a separate 10-minute break countdown. The break auto-resumes the Focus Timer
     * from that exact preserved remaining time when it reaches zero (see the init tick loop). */
    fun takeFocusBreak(): Boolean {
        val overlay = _focusOverlay.value ?: return false
        if (overlay.isOnBreak || overlay.breaksRemaining <= 0) return false
        pauseTimer()
        _focusOverlay.value = overlay.copy(
            breaksRemaining = overlay.breaksRemaining - 1,
            breakEndAtMillis = System.currentTimeMillis() + FOCUS_BREAK_DURATION_MILLIS
        )
        return true
    }
}
