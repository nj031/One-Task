package com.nj031.onetask.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nj031.onetask.data.settings.ColorTheme

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
 * share the palette's primary text color) so every color theme produces a structurally identical
 * ColorScheme, differing only in which values fill these same roles - never introducing a role no
 * screen already reads. Dark mode's surfaceVariant reads the palette's elevatedSurface instead,
 * matching Blue Dark's original distinct "chip" tone (visibly different from its plain surface).
 *
 * Blue Light's error/onError are intentionally left unset here, exactly as they always were
 * before this theme system existed (falling back to Material3's own baseline default) - every
 * other combination explicitly sets error/onError from its own palette.
 */
private fun buildColorScheme(colorTheme: ColorTheme, dark: Boolean): ColorScheme {
    val palette = paletteFor(colorTheme, dark)
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
    } else if (colorTheme == ColorTheme.BLUE) {
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

@Composable
fun OneTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: ColorTheme = ColorTheme.BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(colorTheme, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
