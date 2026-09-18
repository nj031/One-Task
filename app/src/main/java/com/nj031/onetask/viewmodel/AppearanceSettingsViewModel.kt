package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nj031.onetask.data.settings.AppearanceSettingsRepository
import com.nj031.onetask.data.settings.ColorTheme
import com.nj031.onetask.data.settings.DisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs both the Appearance Settings screen and the app-wide OneTaskTheme wrapper in
 * MainActivity - hoisted once there and passed down into OneTaskNavHost (rather than obtained
 * separately in each place) so a color/display-mode change made on the Appearance screen is
 * reflected instantly by the very same ColorScheme every other screen is already composing
 * under, with no separate reload/restart step.
 */
class AppearanceSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppearanceSettingsRepository(application)

    private val _displayMode = MutableStateFlow(repository.getDisplayMode())
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _colorTheme = MutableStateFlow(repository.getColorTheme())
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    fun setDisplayMode(mode: DisplayMode) {
        repository.setDisplayMode(mode)
        _displayMode.value = mode
    }

    fun setColorTheme(theme: ColorTheme) {
        repository.setColorTheme(theme)
        _colorTheme.value = theme
    }
}
