package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.data.settings.TimeFormat
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.data.sync.CloudGeneralSettings
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the General Settings screen and its sub-screens - hoisted once in NavGraph (same
 * pattern as ProfileViewModel/AuthViewModel) so a change made in the Start Screen picker is
 * immediately reflected back on the General list without a separate reload step.
 */
class GeneralSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeneralSettingsRepository(application)

    private val _startScreen = MutableStateFlow(repository.getStartScreen())
    val startScreen: StateFlow<StartScreen> = _startScreen.asStateFlow()

    fun setStartScreen(startScreen: StartScreen) {
        repository.setStartScreen(startScreen)
        _startScreen.value = startScreen
        pushToCloud()
    }

    // --- Default Task Settings - only ever seed a brand-new task's initial fields; changing
    // these never touches any task that already exists. ---
    private val _defaultTimerMinutes = MutableStateFlow(repository.getDefaultTimerMinutes())
    val defaultTimerMinutes: StateFlow<Int?> = _defaultTimerMinutes.asStateFlow()

    private val _defaultTag = MutableStateFlow(repository.getDefaultTag())
    val defaultTag: StateFlow<String?> = _defaultTag.asStateFlow()

    private val _defaultPostponeIfIncomplete = MutableStateFlow(repository.getDefaultPostponeIfIncomplete())
    val defaultPostponeIfIncomplete: StateFlow<Boolean> = _defaultPostponeIfIncomplete.asStateFlow()

    fun setDefaultTimerMinutes(minutes: Int?) {
        repository.setDefaultTimerMinutes(minutes)
        _defaultTimerMinutes.value = minutes
        pushToCloud()
    }

    fun setDefaultTag(tag: String?) {
        repository.setDefaultTag(tag)
        _defaultTag.value = tag
        pushToCloud()
    }

    fun setDefaultPostponeIfIncomplete(postpone: Boolean) {
        repository.setDefaultPostponeIfIncomplete(postpone)
        _defaultPostponeIfIncomplete.value = postpone
        pushToCloud()
    }

    // --- Notifications ---
    private val _focusSessionNotificationsEnabled = MutableStateFlow(repository.getFocusSessionNotificationsEnabled())
    val focusSessionNotificationsEnabled: StateFlow<Boolean> = _focusSessionNotificationsEnabled.asStateFlow()

    private val _focusSessionCompleteEnabled = MutableStateFlow(repository.getFocusSessionCompleteEnabled())
    val focusSessionCompleteEnabled: StateFlow<Boolean> = _focusSessionCompleteEnabled.asStateFlow()

    fun setFocusSessionNotificationsEnabled(enabled: Boolean) {
        repository.setFocusSessionNotificationsEnabled(enabled)
        _focusSessionNotificationsEnabled.value = enabled
        pushToCloud()
    }

    fun setFocusSessionCompleteEnabled(enabled: Boolean) {
        repository.setFocusSessionCompleteEnabled(enabled)
        _focusSessionCompleteEnabled.value = enabled
        pushToCloud()
    }

    // --- Week Starts On / Time Format - purely presentational, never touches task dates,
    // recurring-task logic, or timer duration formatting. ---
    private val _weekStartDay = MutableStateFlow(repository.getWeekStartDay())
    val weekStartDay: StateFlow<DayOfWeek> = _weekStartDay.asStateFlow()

    private val _timeFormat = MutableStateFlow(repository.getTimeFormat())
    val timeFormat: StateFlow<TimeFormat> = _timeFormat.asStateFlow()

    fun setWeekStartDay(day: DayOfWeek) {
        repository.setWeekStartDay(day)
        _weekStartDay.value = day
        pushToCloud()
    }

    fun setTimeFormat(timeFormat: TimeFormat) {
        repository.setTimeFormat(timeFormat)
        _timeFormat.value = timeFormat
        pushToCloud()
    }

    // --- Haptic Feedback ---
    private val _hapticFeedbackEnabled = MutableStateFlow(repository.getHapticFeedbackEnabled())
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        repository.setHapticFeedbackEnabled(enabled)
        _hapticFeedbackEnabled.value = enabled
        pushToCloud()
    }

    /** Account-owned - see the "Future Account-Persistence Architecture Rule": every setting in
     * this ViewModel (Notes View Mode is the one remaining account-owned General Setting, and
     * lives on JournalViewModel instead - see that class) must survive uninstall/reinstall for
     * the same account. The local SharedPreferences write in each setter above already happened
     * synchronously (driving the UI instantly); this only pushes the durable cloud copy, off the
     * calling thread, as one full snapshot (matching how the local file itself stores these
     * settings as one unit, not as independently-synced keys). */
    private fun pushToCloud() {
        viewModelScope.launch {
            val snapshot = repository.getSnapshot()
            CloudBackupRepository.pushGeneralSettings(
                CloudGeneralSettings(
                    startScreen = snapshot.startScreen.name,
                    defaultTimerMinutes = snapshot.defaultTimerMinutes,
                    defaultTag = snapshot.defaultTag,
                    defaultPostponeIfIncomplete = snapshot.defaultPostponeIfIncomplete,
                    focusSessionNotificationsEnabled = snapshot.focusSessionNotificationsEnabled,
                    focusSessionCompleteEnabled = snapshot.focusSessionCompleteEnabled,
                    weekStartDay = snapshot.weekStartDay.name,
                    timeFormat = snapshot.timeFormat.name,
                    hapticFeedbackEnabled = snapshot.hapticFeedbackEnabled,
                    notesViewMode = snapshot.notesViewMode.name,
                    updatedAt = snapshot.updatedAt
                )
            )
        }
    }
}
