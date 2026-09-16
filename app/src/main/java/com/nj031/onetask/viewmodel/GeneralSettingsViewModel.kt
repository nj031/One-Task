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
}
