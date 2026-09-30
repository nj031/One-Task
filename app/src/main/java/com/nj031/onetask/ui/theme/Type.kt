package com.nj031.onetask.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nj031.onetask.R

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// Wallpaper 1's own theme font (see Theme.kt's OneTaskTheme/WallpaperSettingsTheme - only used
// while a wallpaper is active, never for a plain ColorTheme) is Shantell Sans, from the four
// supplied .ttf files. All four faces live in ONE FontFamily (rather than one family per weight)
// so that inline formatting overrides which only set fontWeight/fontStyle without setting
// fontFamily - see NoteEditorScreen's Bold/Italic NoteFormatStyle SpanStyles and its
// FormatGlyphButton toolbar glyphs, neither of which override fontFamily - still resolve to a
// real supplied face instead of Compose synthesizing a fake bold/italic from whichever single
// face the surrounding text's base TextStyle happened to carry.
private val ShantellSansFamily = FontFamily(
    Font(R.font.shantell_sans_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.shantell_sans_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.shantell_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.shantell_sans_semibold_italic, FontWeight.SemiBold, FontStyle.Italic)
)

private fun TextStyle.shantellSans(semiBold: Boolean): TextStyle = copy(
    fontFamily = ShantellSansFamily,
    fontWeight = if (semiBold) FontWeight.SemiBold else FontWeight.Normal
)

/**
 * Wallpaper 1's typography: every Material3 type-scale role the app already reads gets Shantell
 * Sans in one of the spec's two weights, per its "Typography hierarchy" list - page/section/card
 * titles, primary buttons, navigation labels, and the Timer's own display all become SemiBold
 * (600); body text and secondary/meta/disabled text stay Regular (400). Only fontFamily/fontWeight
 * are overridden here - every size/line-height/letter-spacing is Material3's own default (the same
 * ones [Typography] above already implicitly uses for every role but bodyLarge), so this doesn't
 * touch existing font-scaling/accessibility behavior at all.
 */
val WallpaperTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.shantellSans(true),
        displayMedium = base.displayMedium.shantellSans(true),
        displaySmall = base.displaySmall.shantellSans(true),
        headlineLarge = base.headlineLarge.shantellSans(true),
        headlineMedium = base.headlineMedium.shantellSans(true),
        headlineSmall = base.headlineSmall.shantellSans(true),
        titleLarge = base.titleLarge.shantellSans(true),
        titleMedium = base.titleMedium.shantellSans(true),
        titleSmall = base.titleSmall.shantellSans(true),
        bodyLarge = base.bodyLarge.shantellSans(false),
        bodyMedium = base.bodyMedium.shantellSans(false),
        bodySmall = base.bodySmall.shantellSans(false),
        labelLarge = base.labelLarge.shantellSans(true),
        labelMedium = base.labelMedium.shantellSans(false),
        labelSmall = base.labelSmall.shantellSans(false)
    )
}
