package com.nj031.onetask.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nj031.onetask.data.settings.ColorTheme
import com.nj031.onetask.data.settings.Wallpaper

private fun paletteFor(colorTheme: ColorTheme, dark: Boolean): OneTaskColorPalette = when (colorTheme) {
    ColorTheme.BLUE -> if (dark) BlueDarkPalette else BlueLightPalette
    ColorTheme.GREEN -> if (dark) GreenDarkPalette else GreenLightPalette
    ColorTheme.TEAL -> if (dark) TealDarkPalette else TealLightPalette
    ColorTheme.AMBER -> if (dark) AmberDarkPalette else AmberLightPalette
    ColorTheme.PINK -> if (dark) PinkDarkPalette else PinkLightPalette
}

/**
 * Builds the app's actual Material3 ColorScheme from a palette, generalizing the EXACT role
 * mapping Blue Light originally hand-wired (secondary/secondaryContainer/surfaceVariant all
 * share the palette's "light" tint; onSecondary/onSecondaryContainer/onBackground/onSurface all
 * share the palette's primary text color) so every color theme - and, the same way, every
 * wallpaper's own palette (see [OneTaskWallpapers]) - produces a structurally identical
 * ColorScheme, differing only in which values fill these same roles - never introducing a role no
 * screen already reads. Dark mode's surfaceVariant reads the palette's elevatedSurface instead,
 * matching Blue Dark's original distinct "chip" tone (visibly different from its plain surface).
 *
 * [isBlueLight] (true only for the existing Blue Light color theme) leaves error/onError unset
 * here, exactly as they always were before this theme system existed (falling back to Material3's
 * own baseline default) - every other combination, wallpapers included, explicitly sets
 * error/onError from its own palette.
 */
private fun buildColorScheme(palette: OneTaskColorPalette, dark: Boolean, isBlueLight: Boolean = false): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.primaryLight,
            onSecondary = palette.primaryText,
            secondaryContainer = palette.primaryLight,
            onSecondaryContainer = palette.primaryText,
            surfaceVariant = palette.elevatedSurface,
            onSurfaceVariant = palette.secondaryText,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.surface,
            onSurface = palette.primaryText,
            outline = palette.border,
            error = palette.error,
            onError = Color.White
        )
    } else if (isBlueLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.primaryLight,
            onSecondary = palette.primaryText,
            secondaryContainer = palette.primaryLight,
            onSecondaryContainer = palette.primaryText,
            surfaceVariant = palette.primaryLight,
            onSurfaceVariant = palette.secondaryText,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.surface,
            onSurface = palette.primaryText,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.primaryLight,
            onSecondary = palette.primaryText,
            secondaryContainer = palette.primaryLight,
            onSecondaryContainer = palette.primaryText,
            surfaceVariant = palette.primaryLight,
            onSurfaceVariant = palette.secondaryText,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.surface,
            onSurface = palette.primaryText,
            outline = palette.border,
            error = palette.error,
            onError = Color.White
        )
    }
}

private fun buildColorScheme(colorTheme: ColorTheme, dark: Boolean): ColorScheme =
    buildColorScheme(paletteFor(colorTheme, dark), dark, isBlueLight = !dark && colorTheme == ColorTheme.BLUE)

/**
 * The app-wide ColorScheme while [wallpaper] is active (see [OneTaskWallpapers]) - used for
 * every screen except the Settings/Appearance override (see [wallpaperSettingsColorScheme]),
 * including the wallpaper-image screens (Task/Timer/Notes) themselves, matching the Wallpaper
 * spec's "the wallpaper's own palette becomes the active theme" rule. Returns null for
 * [Wallpaper.NONE] - the caller falls back to the existing [ColorTheme]-driven scheme instead.
 */
private fun wallpaperColorScheme(wallpaper: Wallpaper, dark: Boolean): ColorScheme? {
    val definition = OneTaskWallpapers.definitionFor(wallpaper) ?: return null
    val palette = if (dark) definition.dark else definition.light
    return buildColorScheme(palette.colors, dark)
}

/**
 * The distinct palette Settings/Appearance use while a wallpaper is active (see this app's
 * Wallpaper spec, section "Wallpaper theme / palette scope") - every other screen uses
 * [wallpaperColorScheme] instead. Returns null for [Wallpaper.NONE], the same as
 * [wallpaperColorScheme] - callers only need to override the ambient MaterialTheme when this is
 * non-null.
 */
fun wallpaperSettingsColorScheme(wallpaper: Wallpaper, dark: Boolean): ColorScheme? {
    val definition = OneTaskWallpapers.definitionFor(wallpaper) ?: return null
    val palette = if (dark) definition.settingsDark else definition.settingsLight
    return buildColorScheme(palette, dark)
}

/**
 * Wraps [content] (the Settings/Appearance screens only - see this app's Wallpaper spec) with
 * [wallpaperSettingsColorScheme] whenever a wallpaper is active, otherwise a no-op that leaves the
 * ambient [MaterialTheme] (the ordinary [ColorTheme]-driven one) completely untouched.
 */
@Composable
fun WallpaperSettingsTheme(wallpaper: Wallpaper, darkTheme: Boolean, content: @Composable () -> Unit) {
    val overrideScheme = wallpaperSettingsColorScheme(wallpaper, darkTheme)
    if (overrideScheme == null) {
        content()
    } else {
        MaterialTheme(colorScheme = overrideScheme, typography = MaterialTheme.typography, content = content)
    }
}

@Composable
fun OneTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: ColorTheme = ColorTheme.BLUE,
    wallpaper: Wallpaper = Wallpaper.NONE,
    content: @Composable () -> Unit
) {
    // A wallpaper's own palette becomes the active theme app-wide while it's selected - the
    // user's saved Theme Color is never lost (see AppearanceSettingsRepository.getColorTheme's
    // own doc comment), it's simply not the one driving colors right now.
    val colorScheme = wallpaperColorScheme(wallpaper, darkTheme) ?: buildColorScheme(colorTheme, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
