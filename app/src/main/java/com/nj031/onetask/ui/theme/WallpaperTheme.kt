package com.nj031.onetask.ui.theme

import androidx.compose.ui.graphics.Color
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper

/**
 * Every color a single wallpaper's palette (in one display mode) contributes: [colors] feeds the
 * app-wide [MaterialTheme] ColorScheme (see [buildColorScheme] in Theme.kt) exactly like an
 * ordinary [OneTaskColorPalette] already does for a plain [com.nj031.onetask.data.settings.ColorTheme]
 * - [overlay], [bottomNavigation], and [onWallpaperText] are wallpaper-only values that don't
 * correspond to an existing ColorScheme role a screen already reads the same way for a plain color
 * theme: [overlay] is the translucent scrim drawn over the wallpaper image itself (see
 * [com.nj031.onetask.ui.components.WallpaperBackdrop]); [bottomNavigation] is the bottom nav bar's
 * own distinct translucency (kept separate from [colors]' surface/Card translucency, which every
 * other card-like surface already reads); [onWallpaperText] is the color for text/icons sitting
 * directly on the wallpaper image or the plain wallpaper background color (e.g. a screen's own
 * page title) - kept distinct from [OneTaskColorPalette.primaryText] (used for text on top of an
 * opaque Card) because a wallpaper's background and its Card color are not close in lightness the
 * way a plain color theme's background/surface already are, so one shared text color can't serve
 * both roles at readable contrast (see [wallpaperColorScheme] in Theme.kt, which maps this to the
 * ColorScheme's `onBackground` role specifically, leaving `onSurface` on [colors].primaryText).
 */
data class OneTaskWallpaperColors(
    val colors: OneTaskColorPalette,
    val overlay: Color,
    val bottomNavigation: Color,
    val onWallpaperText: Color
)

/**
 * A single wallpaper: a [name] shown in the Wallpaper picker, a Light and Dark image (see the
 * Wallpaper spec's Display Mode rule - every wallpaper needs both), a Light and Dark
 * [OneTaskWallpaperColors] for the rest of the app, and a separate Light/Dark [OneTaskColorPalette]
 * Settings/Appearance use instead (see [wallpaperSettingsColorScheme] in Theme.kt) - the wallpaper
 * IMAGE never shows on those two screens, only this distinct, more muted palette. Adding a future
 * wallpaper means adding one more of these to [OneTaskWallpapers.definitionFor] and one more
 * [Wallpaper] enum constant - nothing here assumes every future wallpaper shares Wallpaper 1's
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
// Wallpaper 1. Exact hex values from the Wallpaper 1 spec. Only BACKGROUND vs
// BACKGROUND_DARK differ between light/dark - every other token the spec gives
// is a single flat value reused for both, matching the reference screenshots
// (Wallpaper 1's own look doesn't otherwise change with Display Mode) and
// matching there being only one supplied wallpaper image (used for both
// lightImageRes/darkImageRes below - see OneTaskWallpaperDefinition's own doc
// comment). `surface`/`elevatedSurface` are fully opaque (CARD/CARD_LIGHT) per
// the spec's explicit "cards are opaque, no wallpaper bleed-through" rule -
// unlike Verdant's translucent Card values, this is a deliberate departure,
// not an oversight. `primary` is PRIMARY_ACCENT (every button/selected-state/
// accent icon in the reference screenshots reads this green). `onWallpaperText`
// is LIGHT_TEXT - see OneTaskWallpaperColors' own doc comment for why this is
// kept distinct from primaryText.
// ----------------------------------------------------------------------------
private val Wallpaper1PrimaryAccent = Color(0xFF39775F) // PRIMARY_ACCENT
private val Wallpaper1CardLight = Color(0xFFE6D4B0) // CARD_LIGHT
private val Wallpaper1Error = Color(0xFFA95C55) // ERROR
private val Wallpaper1Warning = Color(0xFFB08A42) // PRIORITY_MEDIUM, reused as this palette's warning tone

private fun wallpaper1Colors(background: Color) = OneTaskColorPalette(
    primary = Wallpaper1PrimaryAccent,
    primaryDark = Color(0xFF2C5C48), // a darker shade of PRIMARY_ACCENT; not given explicitly by the spec
    primaryLight = Wallpaper1CardLight,
    accent = Wallpaper1PrimaryAccent,
    background = background,
    surface = Color(0xFFE1CFA7), // CARD, fully opaque
    elevatedSurface = Wallpaper1CardLight,
    primaryText = Color(0xFF202A29), // PRIMARY_TEXT
    secondaryText = Color(0xFF68736F), // SECONDARY_TEXT
    mutedText = Color(0xFF596863), // SOFT_OUTLINE
    border = Color(0xFF202B2A), // OUTLINE
    success = Wallpaper1PrimaryAccent,
    error = Wallpaper1Error,
    warning = Wallpaper1Warning,
    disabled = Color(0xFFC8B98F), // DIVIDER
    priorityHigh = Wallpaper1PrimaryAccent, // PRIORITY_HIGH
    priorityMedium = Wallpaper1Warning, // PRIORITY_MEDIUM
    priorityLow = Wallpaper1Error // PRIORITY_LOW
)

private val Wallpaper1LightColors = wallpaper1Colors(background = Color(0xFF35647F)) // BACKGROUND
private val Wallpaper1DarkColors = wallpaper1Colors(background = Color(0xFF2F5B77)) // BACKGROUND_DARK
private val Wallpaper1OnWallpaperText = Color(0xFFE8D7B3) // LIGHT_TEXT

// Do NOT add a scrim/overlay over the supplied wallpaper image (per the spec's own Accessibility/
// Readability section: "Do NOT add a background overlay... preserve the artwork as designed") -
// Cards are opaque already, so no overlay is needed for card-text contrast either.
private val Wallpaper1Light = OneTaskWallpaperColors(
    colors = Wallpaper1LightColors,
    overlay = Color.Transparent,
    bottomNavigation = Wallpaper1CardLight,
    onWallpaperText = Wallpaper1OnWallpaperText
)

private val Wallpaper1Dark = OneTaskWallpaperColors(
    colors = Wallpaper1DarkColors,
    overlay = Color.Transparent,
    bottomNavigation = Wallpaper1CardLight,
    onWallpaperText = Wallpaper1OnWallpaperText
)

private val Wallpaper1Definition = OneTaskWallpaperDefinition(
    id = Wallpaper.WALLPAPER_1,
    lightImageRes = R.drawable.wallpaper_1,
    darkImageRes = R.drawable.wallpaper_1,
    light = Wallpaper1Light,
    dark = Wallpaper1Dark,
    settingsLight = Wallpaper1LightColors,
    settingsDark = Wallpaper1DarkColors
)

/**
 * The registry every selectable [Wallpaper] resolves through - the single place a future
 * wallpaper gets added (a new [OneTaskWallpaperDefinition] plus a new branch here), never by
 * special-casing [Wallpaper.WALLPAPER_1] anywhere else in the app.
 */
object OneTaskWallpapers {
    fun definitionFor(wallpaper: Wallpaper): OneTaskWallpaperDefinition? = when (wallpaper) {
        Wallpaper.NONE -> null
        Wallpaper.WALLPAPER_1 -> Wallpaper1Definition
    }
}
