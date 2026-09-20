package com.nj031.onetask.ui.theme

import androidx.compose.ui.graphics.Color
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper

/**
 * Every color a single wallpaper's palette (in one display mode) contributes: [colors] feeds the
 * app-wide [MaterialTheme] ColorScheme (see [buildColorScheme] in Theme.kt) exactly like an
 * ordinary [OneTaskColorPalette] already does for a plain [com.nj031.onetask.data.settings.ColorTheme]
 * - [overlay] and [bottomNavigation] are the two wallpaper-only values that don't correspond to an
 * existing ColorScheme role a screen already reads: [overlay] is the translucent scrim drawn over
 * the wallpaper image itself (see [com.nj031.onetask.ui.components.WallpaperBackdrop]), and
 * [bottomNavigation] is the bottom nav bar's own distinct translucency (kept separate from
 * [colors]' surface/Card translucency, which every other card-like surface already reads).
 */
data class OneTaskWallpaperColors(
    val colors: OneTaskColorPalette,
    val overlay: Color,
    val bottomNavigation: Color
)

/**
 * A single wallpaper: a [name] shown in the Wallpaper picker, a Light and Dark image (see the
 * Wallpaper spec's Display Mode rule - every wallpaper needs both), a Light and Dark
 * [OneTaskWallpaperColors] for the rest of the app, and a separate Light/Dark [OneTaskColorPalette]
 * Settings/Appearance use instead (see [wallpaperSettingsColorScheme] in Theme.kt) - the wallpaper
 * IMAGE never shows on those two screens, only this distinct, more muted palette. Adding a future
 * wallpaper means adding one more of these to [OneTaskWallpapers.definitionFor] and one more
 * [Wallpaper] enum constant - nothing here assumes every wallpaper is green or shares Verdant's
 * own values.
 */
data class OneTaskWallpaperDefinition(
    val id: Wallpaper,
    val lightImageRes: Int,
    val darkImageRes: Int,
    val light: OneTaskWallpaperColors,
    val dark: OneTaskWallpaperColors,
    val settingsLight: OneTaskColorPalette,
    val settingsDark: OneTaskColorPalette
)

// ----------------------------------------------------------------------------
// Verdant - Light. Exact values from the Wallpaper spec. "Button" (the one
// concrete, exact-opacity UI element the spec names) is what `primary` is
// mapped to here, since every button in this app sources its container color
// from MaterialTheme.colorScheme.primary (see Theme.kt's buildColorScheme) -
// "Primary Green" itself has no other concrete call site the spec names, so
// it backs `accent` instead (reserved, like every color theme's own `accent`
// field already is - see Color.kt). `surface` similarly takes the spec's
// exact translucent "Card" value (not the solid "Card Tint", which becomes
// `elevatedSurface` instead), since every card-like surface in this app
// already reads MaterialTheme.colorScheme.surface.
// ----------------------------------------------------------------------------
private val VerdantLightColors = OneTaskColorPalette(
    primary = Color(red = 95, green = 174, blue = 104, alpha = 224), // Bright Green, Button 88%
    primaryDark = Color(0xFF285B35), // Dark Green
    primaryLight = Color(0xFF78C982), // Accent Green
    accent = Color(0xFF3F7D4A), // Primary Green
    background = Color(0xFFF4F8F3),
    surface = Color(red = 255, green = 255, blue = 255, alpha = 56), // Card 22%
    elevatedSurface = Color(0xFFFFFFFF), // Card Tint
    primaryText = Color(0xFF203027),
    secondaryText = Color(0xFF63746A),
    mutedText = Color(0xFF87968D),
    border = Color(red = 255, green = 255, blue = 255, alpha = 51), // Card Border 20%
    success = Color(0xFF4F9D5B),
    error = Color(0xFFC75C5C),
    warning = Color(0xFFC89535),
    disabled = Color(0xFFB8CCBC) // Border (solid)
)

private val VerdantLightWallpaper = OneTaskWallpaperColors(
    colors = VerdantLightColors,
    overlay = Color(red = 20, green = 55, blue = 28, alpha = 20), // 8%
    bottomNavigation = Color(red = 245, green = 250, blue = 246, alpha = 77) // 30%
)

// ----------------------------------------------------------------------------
// Verdant - Dark
// ----------------------------------------------------------------------------
private val VerdantDarkColors = OneTaskColorPalette(
    primary = Color(red = 99, green = 170, blue = 113, alpha = 209), // Button 82%
    primaryDark = Color(0xFF315B4A), // Dark Green
    primaryLight = Color(0xFFA8E8AE), // Accent Green
    accent = Color(0xFF7FCB8B), // Primary Green
    background = Color(0xFF082B2D),
    surface = Color(red = 30, green = 65, blue = 62, alpha = 51), // Card 20%
    elevatedSurface = Color(0xFF193D3B), // Card Tint
    primaryText = Color(0xFFF1F5F0),
    secondaryText = Color(0xFFB8C8C0),
    mutedText = Color(0xFF8EA49A),
    border = Color(red = 190, green = 225, blue = 205, alpha = 46), // Card Border 18%
    success = Color(0xFF7FCB8B),
    error = Color(0xFFD86B6B),
    warning = Color(0xFFD5AA55),
    disabled = Color(0xFFA8CDB5) // Border (solid)
)

private val VerdantDarkWallpaper = OneTaskWallpaperColors(
    colors = VerdantDarkColors,
    overlay = Color(red = 4, green = 25, blue = 25, alpha = 46), // 18%
    bottomNavigation = Color(red = 20, green = 48, blue = 46, alpha = 71) // 28%
)

// ----------------------------------------------------------------------------
// Verdant - Settings palettes. Only Settings/Appearance read these (see
// wallpaperSettingsColorScheme in Theme.kt) - the wallpaper image never shows
// on either screen. `primary` here is "Settings Primary" itself (not a
// translucent Button value - Settings/Appearance have no such button), and
// `surface` is "Settings Card" (92%/88%), matching this palette's own exact
// opacity section.
// ----------------------------------------------------------------------------
private val VerdantSettingsLightColors = OneTaskColorPalette(
    primary = Color(0xFF3F7D4A),
    primaryDark = Color(0xFF78A981), // Settings Secondary Green
    primaryLight = Color(0xFFE7F1E8), // Icon Background
    accent = Color(0xFF5FAE68), // Settings Accent
    background = Color(0xFFF5F8F4),
    surface = Color(red = 255, green = 255, blue = 255, alpha = 235), // Settings Card 92%
    elevatedSurface = Color(0xFFFFFFFF), // Settings Surface
    primaryText = Color(0xFF1F3025),
    secondaryText = Color(0xFF66766B),
    mutedText = Color(0xFF8A968E),
    border = Color(0xFFC5D4C8), // Settings Border
    success = VerdantLightColors.success,
    error = VerdantLightColors.error,
    warning = VerdantLightColors.warning,
    disabled = Color(0xFFD8E2D9) // Settings Divider
)

private val VerdantSettingsDarkColors = OneTaskColorPalette(
    primary = Color(0xFF7FCB8B),
    primaryDark = Color(0xFF315B4A), // Settings Dark Green
    primaryLight = Color(0xFF193D3B), // Icon Background
    accent = Color(0xFF9BE2A5), // Settings Accent
    background = Color(0xFF071F21),
    surface = Color(red = 16, green = 47, blue = 48, alpha = 224), // Settings Card 88%
    elevatedSurface = Color(0xFF163A38), // Settings Secondary Surface
    primaryText = Color(0xFFF1F5F0),
    secondaryText = Color(0xFFB8C8C0),
    mutedText = Color(0xFF82988E),
    border = Color(0xFF42645A), // Settings Border
    success = VerdantDarkColors.success,
    error = VerdantDarkColors.error,
    warning = VerdantDarkColors.warning,
    disabled = Color(0xFF294542) // Settings Divider
)

private val VerdantDefinition = OneTaskWallpaperDefinition(
    id = Wallpaper.VERDANT,
    lightImageRes = R.drawable.wallpaper_verdant_light,
    darkImageRes = R.drawable.wallpaper_verdant_dark,
    light = VerdantLightWallpaper,
    dark = VerdantDarkWallpaper,
    settingsLight = VerdantSettingsLightColors,
    settingsDark = VerdantSettingsDarkColors
)

/**
 * The registry every selectable [Wallpaper] resolves through - the single place a future
 * wallpaper gets added (a new [OneTaskWallpaperDefinition] plus a new branch here), never by
 * special-casing [Wallpaper.VERDANT] anywhere else in the app.
 */
object OneTaskWallpapers {
    fun definitionFor(wallpaper: Wallpaper): OneTaskWallpaperDefinition? = when (wallpaper) {
        Wallpaper.NONE -> null
        Wallpaper.VERDANT -> VerdantDefinition
    }
}
