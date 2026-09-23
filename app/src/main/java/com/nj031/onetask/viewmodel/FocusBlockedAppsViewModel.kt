package com.nj031.onetask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.blocking.DISCORD_PACKAGE
import com.nj031.onetask.data.blocking.DISTRACTING_APP_PACKAGES
import com.nj031.onetask.data.blocking.InstalledApp
import com.nj031.onetask.data.blocking.TELEGRAM_PACKAGE
import com.nj031.onetask.data.blocking.findInstalledApp
import com.nj031.onetask.data.blocking.queryLaunchableUserApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the Focus Mode Configuration screen's Block Distractions preview AND the Block
 * Distracting Apps picker it opens (see NavGraph - both share this one instance, scoped to the
 * Focus Mode Configuration route's own back stack entry, the same pattern TimerViewModel already
 * uses to share state between the Timer tab and this same Configuration screen).
 *
 * [selectedPackages] is the one piece of real state here - which actual installed apps the user
 * has chosen to block - and is deliberately in-memory only for this phase (lost on process death,
 * same tradeoff already approved for FocusOverlayState): it lives exactly as long as the Focus
 * Mode Configuration screen itself does, which is enough to "remain selected when reopening the
 * [picker] screen" per spec, since reopening the picker never destroys this ViewModel.
 *
 * The three installed-app lists below are loaded once, off the main thread (PackageManager scans
 * can be slow on a device with many apps), and never change afterward within one ViewModel
 * instance - a fresh Configuration visit gets a fresh scan, matching every other piece of this
 * screen's own session-scoped (not persisted) state.
 */
class FocusBlockedAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val _distractingApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val distractingApps: StateFlow<List<InstalledApp>> = _distractingApps.asStateFlow()

    private val _discordApp = MutableStateFlow<InstalledApp?>(null)
    val discordApp: StateFlow<InstalledApp?> = _discordApp.asStateFlow()

    private val _telegramApp = MutableStateFlow<InstalledApp?>(null)
    val telegramApp: StateFlow<InstalledApp?> = _telegramApp.asStateFlow()

    private val _otherApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val otherApps: StateFlow<List<InstalledApp>> = _otherApps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    init {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val (distracting, discord, telegram, others) = withContext(Dispatchers.Default) {
                val distracting = DISTRACTING_APP_PACKAGES.mapNotNull { findInstalledApp(context, it) }
                val discord = findInstalledApp(context, DISCORD_PACKAGE)
                val telegram = findInstalledApp(context, TELEGRAM_PACKAGE)
                val explicitlyListed = DISTRACTING_APP_PACKAGES.toSet() + DISCORD_PACKAGE + TELEGRAM_PACKAGE
                val others = queryLaunchableUserApps(context, excludedPackages = explicitlyListed)
                FourLists(distracting, discord, telegram, others)
            }
            _distractingApps.value = distracting
            _discordApp.value = discord
            _telegramApp.value = telegram
            _otherApps.value = others
        }
    }

    private data class FourLists(
        val distracting: List<InstalledApp>,
        val discord: InstalledApp?,
        val telegram: InstalledApp?,
        val others: List<InstalledApp>
    )

    fun toggleSelected(packageName: String) {
        _selectedPackages.value = _selectedPackages.value.let {
            if (packageName in it) it - packageName else it + packageName
        }
    }

    fun setSelected(packageNames: Collection<String>, selected: Boolean) {
        _selectedPackages.value = if (selected) {
            _selectedPackages.value + packageNames
        } else {
            _selectedPackages.value - packageNames.toSet()
        }
    }
}
