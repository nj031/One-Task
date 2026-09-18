package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.timer.DEFAULT_TIMER_DURATION_MILLIS
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
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimerSessionRepository(application)

    private val _snapshot = MutableStateFlow(repository.snapshot())
    val snapshot: StateFlow<TimerSessionSnapshot> = _snapshot.asStateFlow()

    private val _selectedIdleDurationMillis = MutableStateFlow(DEFAULT_TIMER_DURATION_MILLIS)
    val selectedIdleDurationMillis: StateFlow<Long> = _selectedIdleDurationMillis.asStateFlow()

    private val _showCompletionPopup = MutableStateFlow(false)
    val showCompletionPopup: StateFlow<Boolean> = _showCompletionPopup.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val current = repository.snapshot()
                if (current.activeMode == TimerMode.TIMER && repository.finishTimerIfDue()) {
                    _showCompletionPopup.value = true
                    _snapshot.value = repository.snapshot()
                } else {
                    _snapshot.value = current
                }
                delay(if (current.activeMode != null) 250L else 2_000L)
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

    fun dismissCompletionPopup() {
        _showCompletionPopup.value = false
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
}
