package com.nj031.onetask.data.settings

import android.content.Context

enum class StartScreen { TASKS, JOURNAL }

private const val PREFS_NAME = "general_settings_prefs"
private const val KEY_START_SCREEN = "start_screen"

/**
 * Stores General Settings (currently just Start Screen) in a private SharedPreferences file,
 * the same choice made for UserProfileRepository: AppDatabase has no real Migration objects and
 * falls back to fallbackToDestructiveMigration() on any version bump, so a new Room table for a
 * couple of simple preference values isn't worth risking every existing install's Tasks/Journal
 * data on their next update.
 */
class GeneralSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Tasks is the default for both existing and new users - anyone who hasn't visited this
     * setting yet keeps launching straight into Tasks, exactly as before this setting existed. */
    fun getStartScreen(): StartScreen {
        val raw = prefs.getString(KEY_START_SCREEN, null) ?: return StartScreen.TASKS
        return runCatching { StartScreen.valueOf(raw) }.getOrDefault(StartScreen.TASKS)
    }

    fun setStartScreen(startScreen: StartScreen) {
        prefs.edit().putString(KEY_START_SCREEN, startScreen.name).apply()
    }
}
