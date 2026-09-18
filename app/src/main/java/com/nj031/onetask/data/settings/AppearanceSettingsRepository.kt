package com.nj031.onetask.data.settings

import android.content.Context

/** Follows the device's own light/dark setting, or forces one or the other. */
enum class DisplayMode { SYSTEM, LIGHT, DARK }

/** The app's selectable primary color themes. Purple was never shipped; Pink is its
 * permanent replacement - there is no code path that can select Purple. */
enum class ColorTheme { BLUE, GREEN, TEAL, AMBER, PINK }

private const val PREFS_NAME = "appearance_settings_prefs"
private const val KEY_DISPLAY_MODE = "display_mode"
private const val KEY_COLOR_THEME = "color_theme"

/**
 * Stores Appearance Settings (Display Mode, Color Theme) in a private SharedPreferences file,
 * the same choice GeneralSettingsRepository already made: AppDatabase has no real Migration
 * objects and falls back to fallbackToDestructiveMigration() on any version bump, so a new Room
 * table for two simple preference values isn't worth risking every existing install's data.
 *
 * SYSTEM/BLUE are the defaults - both exactly match this app's behavior before Appearance
 * Settings existed (OneTaskTheme's own prior default was `isSystemInDarkTheme()`, and Blue was
 * the only color the app ever had), so an existing user who has never opened this screen sees
 * no change at all.
 */
class AppearanceSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayMode(): DisplayMode {
        val raw = prefs.getString(KEY_DISPLAY_MODE, null) ?: return DisplayMode.SYSTEM
        return runCatching { DisplayMode.valueOf(raw) }.getOrDefault(DisplayMode.SYSTEM)
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
    }

    fun getColorTheme(): ColorTheme {
        val raw = prefs.getString(KEY_COLOR_THEME, null) ?: return ColorTheme.BLUE
        return runCatching { ColorTheme.valueOf(raw) }.getOrDefault(ColorTheme.BLUE)
    }

    fun setColorTheme(theme: ColorTheme) {
        prefs.edit().putString(KEY_COLOR_THEME, theme.name).apply()
    }
}
