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

// One Task's global light theme: a single, consistent light-blue/navy visual identity
// shared by every screen (not a per-screen override) - see Color.kt for the token values.
private val LightColorScheme = lightColorScheme(
    primary = OneTaskPrimary,
    onPrimary = Color.White,
    secondary = OneTaskLightBlue,
    onSecondary = OneTaskDarkText,
    secondaryContainer = OneTaskLightBlue,
    onSecondaryContainer = OneTaskDarkText,
    surfaceVariant = OneTaskLightBlue,
    onSurfaceVariant = OneTaskSecondaryText,
    background = OneTaskBackground,
    onBackground = OneTaskDarkText,
    surface = OneTaskSurface,
    onSurface = OneTaskDarkText,
    outline = OneTaskBorder
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
