package com.nj031.onetask.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// Wallpaper 1's own theme font is meant to be Mali (see Theme.kt's OneTaskTheme/
// WallpaperSettingsTheme - only used while a wallpaper is active, never for a plain ColorTheme),
// but the actual Mali typeface could NOT be wired in as part of this change - see this change's
// own final report. Neither a local .ttf (no network access in this sandbox to fetch Google
// Fonts' font files) nor Android's declarative downloadable-font mechanism (which requires the
// Google Play services Fonts provider's exact signing-certificate hashes, a multi-KB value that
// can't be verified without network access either - shipping a guessed one would be worse than
// this: font-family provider mismatches fail differently from a wrong typeface, and can throw
// rather than gracefully falling back) could be safely produced here. FontFamily.Default is used
// instead so the WEIGHT hierarchy the spec calls for (only Regular/400 and SemiBold/600) is still
// correct even though the actual glyph shapes aren't Mali yet - swap MaliRegular/MaliSemiBold
// below for the real font once either a local .ttf or a verified cert array is available.
private val MaliRegular = FontFamily.Default
private val MaliSemiBold = FontFamily.Default

private fun TextStyle.mali(semiBold: Boolean): TextStyle = copy(
    fontFamily = if (semiBold) MaliSemiBold else MaliRegular,
    fontWeight = if (semiBold) FontWeight.SemiBold else FontWeight.Normal
)

/**
 * Wallpaper 1's typography: every Material3 type-scale role the app already reads gets Mali in
 * one of the spec's two weights, per its "Typography hierarchy" list - page/section/card titles,
 * primary buttons, navigation labels, and the Timer's own display all become SemiBold (600); body
 * text and secondary/meta/disabled text stay Regular (400). Only fontFamily/fontWeight are
 * overridden here - every size/line-height/letter-spacing is Material3's own default (the same
 * ones [Typography] above already implicitly uses for every role but bodyLarge), so this doesn't
 * touch existing font-scaling/accessibility behavior at all.
 */
val WallpaperTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.mali(true),
        displayMedium = base.displayMedium.mali(true),
        displaySmall = base.displaySmall.mali(true),
        headlineLarge = base.headlineLarge.mali(true),
        headlineMedium = base.headlineMedium.mali(true),
        headlineSmall = base.headlineSmall.mali(true),
        titleLarge = base.titleLarge.mali(true),
        titleMedium = base.titleMedium.mali(true),
        titleSmall = base.titleSmall.mali(true),
        bodyLarge = base.bodyLarge.mali(false),
        bodyMedium = base.bodyMedium.mali(false),
        bodySmall = base.bodySmall.mali(false),
        labelLarge = base.labelLarge.mali(true),
        labelMedium = base.labelMedium.mali(false),
        labelSmall = base.labelSmall.mali(false)
    )
}
