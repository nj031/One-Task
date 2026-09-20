package com.nj031.onetask.data.settings

import android.content.Context
import com.nj031.onetask.data.UserScopedPreferences

/** Follows the device's own light/dark setting, or forces one or the other. */
enum class DisplayMode { SYSTEM, LIGHT, DARK }

/** The app's selectable primary color themes. Purple was never shipped; Pink is its
 * permanent replacement - there is no code path that can select Purple. */
enum class ColorTheme { BLUE, GREEN, TEAL, AMBER, PINK }

/** A selectable Wallpaper - NONE means the existing plain [ColorTheme] system is active (see
 * [OneTaskWallpaperDefinition]'s own doc comment for the Display Mode/Theme Color interaction
 * rules a wallpaper follows). Every wallpaper other than NONE must have a matching
 * [OneTaskWallpaperDefinition] registered in [com.nj031.onetask.ui.theme.OneTaskWallpapers] -
 * adding a future wallpaper means adding both a new constant here and a new definition there,
 * never changing this enum's existing meaning. */
enum class Wallpaper { NONE, VERDANT }

private const val PREFS_NAME = "appearance_settings_prefs"
private const val KEY_DISPLAY_MODE = "display_mode"
private const val KEY_COLOR_THEME = "color_theme"
private const val KEY_WALLPAPER = "wallpaper"
private const val KEY_UPDATED_AT = "updated_at"

/** A point-in-time snapshot of every field this account's Appearance settings sync to the cloud
 * as one unit (see [CloudBackupRepository.pushAppearance]/[CloudBackupRepository.pullAppearance]) -
 * [updatedAt] is what lets the post-sign-in restore decide whether the local or cloud copy is
 * newer when both already exist, the same way every Task/Note row's own updatedAt already
 * decides which side of a sync should win for that item. [colorTheme] is always preserved as-is
 * here even while [wallpaper] is active (see [getColorTheme]'s own doc comment) - a wallpaper
 * overrides which palette is visually active, it never overwrites the user's saved Theme Color. */
data class AppearanceSnapshot(
    val displayMode: DisplayMode,
    val colorTheme: ColorTheme,
    val wallpaper: Wallpaper,
    val updatedAt: Long
)

/**
 * Stores Appearance Settings (Display Mode, Color Theme, Wallpaper) in a private SharedPreferences
 * file, the same choice GeneralSettingsRepository already made: AppDatabase has no real Migration
 * objects and falls back to fallbackToDestructiveMigration() on any version bump, so a new Room
 * table for a handful of simple preference values isn't worth risking every existing install's
 * data.
 *
 * SYSTEM/BLUE/NONE are the defaults - all three exactly match this app's behavior before
 * Appearance Settings existed (OneTaskTheme's own prior default was `isSystemInDarkTheme()`, Blue
 * was the only color the app ever had, and no wallpaper concept existed at all), so an existing
 * user who has never opened this screen sees no change at all. The file itself is scoped per
 * signed-in account (see
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

    /** The user's saved Theme Color - kept and returned as-is regardless of whether a wallpaper
     * is currently active, so selecting a wallpaper (or later returning to "No Wallpaper") never
     * loses or resets it (see the Wallpaper spec's "preserve the user's saved theme" rule). Only
     * [getWallpaper] decides whether this value is the one actually driving the app's colors
     * right now. */
    fun getColorTheme(): ColorTheme {
        val raw = prefs.getString(KEY_COLOR_THEME, null) ?: return ColorTheme.BLUE
        return runCatching { ColorTheme.valueOf(raw) }.getOrDefault(ColorTheme.BLUE)
    }

    fun setColorTheme(theme: ColorTheme) {
        prefs.edit().putString(KEY_COLOR_THEME, theme.name).putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    fun getWallpaper(): Wallpaper {
        val raw = prefs.getString(KEY_WALLPAPER, null) ?: return Wallpaper.NONE
        return runCatching { Wallpaper.valueOf(raw) }.getOrDefault(Wallpaper.NONE)
    }

    /** Only ever changes [KEY_WALLPAPER] - deliberately never touches [KEY_COLOR_THEME], so the
     * previously saved Theme Color is still there, unchanged, the moment the user switches back
     * to "No Wallpaper" (see [getColorTheme]'s own doc comment). */
    fun setWallpaper(wallpaper: Wallpaper) {
        prefs.edit().putString(KEY_WALLPAPER, wallpaper.name).putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    fun getUpdatedAt(): Long = prefs.getLong(KEY_UPDATED_AT, 0L)

    fun getSnapshot(): AppearanceSnapshot =
        AppearanceSnapshot(getDisplayMode(), getColorTheme(), getWallpaper(), getUpdatedAt())

    /** Applies a cloud-restored Appearance snapshot locally - used only by the post-sign-in
     * restore, never by the Appearance screen's own chip taps (which go through
     * [setDisplayMode]/[setColorTheme]/[setWallpaper] above instead, each bumping its own fresh
     * [KEY_UPDATED_AT]). Writes [updatedAt] as given (the cloud value being restored), not
     * "now", so this restore is never mistaken for a newer local edit on the very next sync. */
    fun applyRemote(displayMode: DisplayMode, colorTheme: ColorTheme, wallpaper: Wallpaper, updatedAt: Long) {
        prefs.edit()
            .putString(KEY_DISPLAY_MODE, displayMode.name)
            .putString(KEY_COLOR_THEME, colorTheme.name)
            .putString(KEY_WALLPAPER, wallpaper.name)
            .putLong(KEY_UPDATED_AT, updatedAt)
            .apply()
    }
}
