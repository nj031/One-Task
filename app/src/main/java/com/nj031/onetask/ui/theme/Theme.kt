package com.nj031.onetask.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color(0xFF042144),
    secondary = LightBlueButtonDark,
    onSecondary = OnLightBlueButton,
    secondaryContainer = AddTaskContainerDark,
    onSecondaryContainer = OnAddTaskContainerDark,
    surfaceVariant = ChipUnselectedDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = Color.White,
    secondary = LightBlueButtonLight,
    onSecondary = OnLightBlueButton,
    secondaryContainer = AddTaskContainerLight,
    onSecondaryContainer = OnAddTaskContainerLight,
    surfaceVariant = ChipUnselectedLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight
)

@Composable
fun OneTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
