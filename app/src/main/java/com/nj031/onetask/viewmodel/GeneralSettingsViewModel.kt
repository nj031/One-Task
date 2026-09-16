package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.settings.StartScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    }

    fun setDefaultTag(tag: String?) {
        repository.setDefaultTag(tag)
        _defaultTag.value = tag
    }

    fun setDefaultPostponeIfIncomplete(postpone: Boolean) {
        repository.setDefaultPostponeIfIncomplete(postpone)
        _defaultPostponeIfIncomplete.value = postpone
    }

    // --- Notifications ---
    private val _focusSessionNotificationsEnabled = MutableStateFlow(repository.getFocusSessionNotificationsEnabled())
    val focusSessionNotificationsEnabled: StateFlow<Boolean> = _focusSessionNotificationsEnabled.asStateFlow()

    private val _focusSessionCompleteEnabled = MutableStateFlow(repository.getFocusSessionCompleteEnabled())
    val focusSessionCompleteEnabled: StateFlow<Boolean> = _focusSessionCompleteEnabled.asStateFlow()

    fun setFocusSessionNotificationsEnabled(enabled: Boolean) {
        repository.setFocusSessionNotificationsEnabled(enabled)
        _focusSessionNotificationsEnabled.value = enabled
    }

    fun setFocusSessionCompleteEnabled(enabled: Boolean) {
        repository.setFocusSessionCompleteEnabled(enabled)
        _focusSessionCompleteEnabled.value = enabled
    }
}
