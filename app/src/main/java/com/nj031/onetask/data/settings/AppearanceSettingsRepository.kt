package com.nj031.onetask.data.settings

import android.content.Context
import com.nj031.onetask.data.UserScopedPreferences

/** Follows the device's own light/dark setting, or forces one or the other. */
enum class DisplayMode { SYSTEM, LIGHT, DARK }

/** The app's selectable primary color themes. Purple was never shipped; Pink is its
 * permanent replacement - there is no code path that can select Purple. */
enum class ColorTheme { BLUE, GREEN, TEAL, AMBER, PINK }

private const val PREFS_NAME = "appearance_settings_prefs"
private const val KEY_DISPLAY_MODE = "display_mode"
private const val KEY_COLOR_THEME = "color_theme"
private const val KEY_UPDATED_AT = "updated_at"

/** A point-in-time snapshot of every field this account's Appearance settings sync to the cloud
 * as one unit (see [CloudBackupRepository.pushAppearance]/[CloudBackupRepository.pullAppearance]) -
 * [updatedAt] is what lets the post-sign-in restore decide whether the local or cloud copy is
 * newer when both already exist, the same way every Task/Note row's own updatedAt already
 * decides which side of a sync should win for that item. */
data class AppearanceSnapshot(val displayMode: DisplayMode, val colorTheme: ColorTheme, val updatedAt: Long)

/**
 * Stores Appearance Settings (Display Mode, Color Theme) in a private SharedPreferences file,
 * the same choice GeneralSettingsRepository already made: AppDatabase has no real Migration
 * objects and falls back to fallbackToDestructiveMigration() on any version bump, so a new Room
 * table for two simple preference values isn't worth risking every existing install's data.
 *
 * SYSTEM/BLUE are the defaults - both exactly match this app's behavior before Appearance
 * Settings existed (OneTaskTheme's own prior default was `isSystemInDarkTheme()`, and Blue was
 * the only color the app ever had), so an existing user who has never opened this screen sees
 * no change at all. The file itself is scoped per signed-in account (see
 * [UserScopedPreferences]) - a plain fixed file name would mean every account on this device
 * shared the exact same Display Mode/Color Theme selection.
 */
class AppearanceSettingsRepository(context: Context) {
    private val prefs = UserScopedPreferences.open(context, PREFS_NAME)

    fun getDisplayMode(): DisplayMode {
        val raw = prefs.getString(KEY_DISPLAY_MODE, null) ?: return DisplayMode.SYSTEM
        return runCatching { DisplayMode.valueOf(raw) }.getOrDefault(DisplayMode.SYSTEM)
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    fun getColorTheme(): ColorTheme {
        val raw = prefs.getString(KEY_COLOR_THEME, null) ?: return ColorTheme.BLUE
        return runCatching { ColorTheme.valueOf(raw) }.getOrDefault(ColorTheme.BLUE)
    }

    fun setColorTheme(theme: ColorTheme) {
        prefs.edit().putString(KEY_COLOR_THEME, theme.name).putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    fun getUpdatedAt(): Long = prefs.getLong(KEY_UPDATED_AT, 0L)

    fun getSnapshot(): AppearanceSnapshot = AppearanceSnapshot(getDisplayMode(), getColorTheme(), getUpdatedAt())

    /** Applies a cloud-restored Appearance snapshot locally - used only by the post-sign-in
     * restore, never by the Appearance screen's own chip taps (which go through
     * [setDisplayMode]/[setColorTheme] above instead, each bumping its own fresh
     * [KEY_UPDATED_AT]). Writes [updatedAt] as given (the cloud value being restored), not
     * "now", so this restore is never mistaken for a newer local edit on the very next sync. */
    fun applyRemote(displayMode: DisplayMode, colorTheme: ColorTheme, updatedAt: Long) {
        prefs.edit()
            .putString(KEY_DISPLAY_MODE, displayMode.name)
            .putString(KEY_COLOR_THEME, colorTheme.name)
            .putLong(KEY_UPDATED_AT, updatedAt)
            .apply()
    }
}
