package com.nj031.onetask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.ColorTheme
import com.nj031.onetask.data.settings.DisplayMode
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.AmberLightPalette
import com.nj031.onetask.ui.theme.BlueLightPalette
import com.nj031.onetask.ui.theme.GreenLightPalette
import com.nj031.onetask.ui.theme.OneTaskMoonIcon
import com.nj031.onetask.ui.theme.OneTaskSunIcon
import com.nj031.onetask.ui.theme.OneTaskSystemDefaultIcon
import com.nj031.onetask.ui.theme.OneTaskWallpaperPlaceholderIcon
import com.nj031.onetask.ui.theme.PinkLightPalette
import com.nj031.onetask.ui.theme.TealLightPalette

/**
 * Profile & Settings > General > Appearance. Both Display Mode and Color Theme write through
 * immediately on tap (no Apply/Save step, no confirmation) - the caller (NavGraph, backed by
 * AppearanceSettingsViewModel) re-renders the ENTIRE app's MaterialTheme.colorScheme the instant
 * either changes, so this screen itself visibly recolors along with everything else. Wallpaper is
 * a structural placeholder only per spec: its tiles are not clickable and carry no state at all,
 * the same "zero click wiring" pattern already used for the Timer screen's Focus Mode placeholder
 * button - "does nothing yet" is enforced at the Compose level, not just by convention.
 */
@Composable
fun AppearanceSettingsScreen(
    displayMode: DisplayMode,
    colorTheme: ColorTheme,
    onSelectDisplayMode: (DisplayMode) -> Unit,
    onSelectColorTheme: (ColorTheme) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.appearance_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.appearance_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 24.dp)
                )

                AppearanceSectionHeader(
                    title = stringResource(id = R.string.appearance_display_mode_title),
                    description = stringResource(id = R.string.appearance_display_mode_description)
                )
                DisplayModeRow(
                    selected = displayMode,
                    onSelect = onSelectDisplayMode,
                    modifier = Modifier.padding(top = 12.dp)
                )

                AppearanceSectionHeader(
                    title = stringResource(id = R.string.appearance_theme_title),
                    description = stringResource(id = R.string.appearance_theme_description),
                    modifier = Modifier.padding(top = 28.dp)
                )
                ColorThemeRow(
                    selected = colorTheme,
                    onSelect = onSelectColorTheme,
                    modifier = Modifier.padding(top = 14.dp)
                )

                AppearanceSectionHeader(
                    title = stringResource(id = R.string.appearance_wallpaper_title),
                    description = stringResource(id = R.string.appearance_wallpaper_description),
                    modifier = Modifier.padding(top = 28.dp)
                )
                WallpaperGrid(modifier = Modifier.padding(top = 14.dp))

                Text(
                    text = stringResource(id = R.string.appearance_wallpaper_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun AppearanceSectionHeader(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun DisplayModeRow(
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DisplayModeCard(
            icon = { tint -> OneTaskSystemDefaultIcon(tint = tint, size = 24.dp) },
            label = stringResource(id = R.string.appearance_display_mode_system),
            selected = selected == DisplayMode.SYSTEM,
            onClick = { onSelect(DisplayMode.SYSTEM) },
            modifier = Modifier.weight(1f)
        )
        DisplayModeCard(
            icon = { tint -> OneTaskSunIcon(tint = tint, size = 24.dp) },
            label = stringResource(id = R.string.appearance_display_mode_light),
            selected = selected == DisplayMode.LIGHT,
            onClick = { onSelect(DisplayMode.LIGHT) },
            modifier = Modifier.weight(1f)
        )
        DisplayModeCard(
            icon = { tint -> OneTaskMoonIcon(tint = tint, size = 24.dp) },
            label = stringResource(id = R.string.appearance_display_mode_dark),
            selected = selected == DisplayMode.DARK,
            onClick = { onSelect(DisplayMode.DARK) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DisplayModeCard(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticTick = rememberHapticTick()
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .selectable(selected = selected, onClick = { hapticTick(); onClick() })
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private data class ColorThemeOption(val theme: ColorTheme, val swatch: Color, val labelRes: Int)

private val COLOR_THEME_OPTIONS = listOf(
    ColorThemeOption(ColorTheme.BLUE, BlueLightPalette.primary, R.string.appearance_theme_blue),
    ColorThemeOption(ColorTheme.GREEN, GreenLightPalette.primary, R.string.appearance_theme_green),
    ColorThemeOption(ColorTheme.TEAL, TealLightPalette.primary, R.string.appearance_theme_teal),
    ColorThemeOption(ColorTheme.AMBER, AmberLightPalette.primary, R.string.appearance_theme_amber),
    ColorThemeOption(ColorTheme.PINK, PinkLightPalette.primary, R.string.appearance_theme_pink)
)

@Composable
private fun ColorThemeRow(
    selected: ColorTheme,
    onSelect: (ColorTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        COLOR_THEME_OPTIONS.forEach { option ->
            ColorThemeSwatch(
                swatchColor = option.swatch,
                label = stringResource(id = option.labelRes),
                selected = option.theme == selected,
                onClick = { onSelect(option.theme) }
            )
        }
    }
}

@Composable
private fun ColorThemeSwatch(
    swatchColor: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val hapticTick = rememberHapticTick()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .selectable(selected = selected, onClick = { hapticTick(); onClick() })
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private const val WALLPAPER_OPTION_COUNT = 5

@Composable
private fun WallpaperGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WallpaperTile(
                label = stringResource(id = R.string.appearance_wallpaper_none),
                selected = true,
                modifier = Modifier.weight(1f)
            )
            for (i in 1..2) {
                WallpaperTile(
                    label = stringResource(id = R.string.appearance_wallpaper_option_format, i),
                    selected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 3..WALLPAPER_OPTION_COUNT) {
                WallpaperTile(
                    label = stringResource(id = R.string.appearance_wallpaper_option_format, i),
                    selected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** A Wallpaper option tile - deliberately has no click handler at all (not even a no-op one), so
 * it structurally cannot activate any wallpaper behavior, per spec ("placeholder only, no
 * activation logic"). [selected] is a purely visual, hardcoded flag (only "No Wallpaper" is ever
 * true) rather than real state, since nothing else is implemented yet to actually be selected. */
@Composable
private fun WallpaperTile(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OneTaskWallpaperPlaceholderIcon(
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 26.dp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
