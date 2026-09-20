package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.settings.AppearanceSettingsRepository
import com.nj031.onetask.data.settings.ColorTheme
import com.nj031.onetask.data.settings.DisplayMode
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.data.sync.CloudAppearance
import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _wallpaper = MutableStateFlow(repository.getWallpaper())
    val wallpaper: StateFlow<Wallpaper> = _wallpaper.asStateFlow()

    fun setDisplayMode(mode: DisplayMode) {
        repository.setDisplayMode(mode)
        _displayMode.value = mode
        pushToCloud()
    }

    fun setColorTheme(theme: ColorTheme) {
        repository.setColorTheme(theme)
        _colorTheme.value = theme
        pushToCloud()
    }

    /** Never touches the saved Theme Color (see [AppearanceSettingsRepository.setWallpaper]'s own
     * doc comment) - selecting a wallpaper only changes which one is visually active right now. */
    fun setWallpaper(wallpaper: Wallpaper) {
        repository.setWallpaper(wallpaper)
        _wallpaper.value = wallpaper
        pushToCloud()
    }

    /** Account-owned - see the "Future Account-Persistence Architecture Rule": Appearance must
     * survive uninstall/reinstall for the same account, not just this installation. The local
     * SharedPreferences write above already happened synchronously (driving the UI instantly);
     * this only pushes the durable cloud copy, off the calling thread. */
    private fun pushToCloud() {
        viewModelScope.launch {
            CloudBackupRepository.pushAppearance(
                CloudAppearance(
                    displayMode = repository.getDisplayMode().name,
                    colorTheme = repository.getColorTheme().name,
                    wallpaper = repository.getWallpaper().name,
                    updatedAt = repository.getUpdatedAt()
                )
            )
        }
    }
}
