package com.nj031.onetask.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.ui.theme.OneTaskWallpaperPlaceholderIcon

private const val WALLPAPER_OPTION_COUNT = 5

/**
 * Profile & Settings > General > Appearance > Wallpaper - the dedicated Wallpaper picker, reached
 * from AppearanceSettingsScreen's own "Wallpaper" navigation row (the grid used to live inline on
 * Appearance itself; only its location changed, not its selection mechanism or persistence - see
 * AppearanceSettingsRepository/AppearanceSettingsViewModel, reused as-is). "No Wallpaper" and
 * "Wallpaper 1" ([Wallpaper.WALLPAPER_1] - see WallpaperTheme.kt) are the only real, selectable
 * options; every other numbered "Wallpaper N" tile stays the exact same non-clickable, stateless
 * placeholder reserved for a future wallpaper - "does nothing yet" is enforced at the Compose
 * level for those, not just by convention.
 */
@Composable
fun WallpaperScreen(
    selected: Wallpaper,
    onSelect: (Wallpaper) -> Unit,
    onBackClick: () -> Unit,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    if (wallpaper == Wallpaper.WALLPAPER_1) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_wp1_back),
                            contentDescription = stringResource(id = R.string.back),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.appearance_wallpaper_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.appearance_wallpaper_description),
                style = MaterialTheme.typography.bodyMedium,
                // This sits directly on the raw wallpaper background (no Card), so
                // onSurfaceVariant (tuned for an opaque Card) reads poorly under Wallpaper 1 -
                // fall back to the same dimmed onBackground treatment used for other
                // raw-background secondary text elsewhere in the app. Every other theme is
                // unaffected.
                color = if (wallpaper != Wallpaper.NONE) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 52.dp, top = 4.dp, bottom = 20.dp)
            )

            WallpaperGrid(selected = selected, onSelect = onSelect)

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

/** 2-column wallpaper grid (never 3, per spec): the first row is always [No Wallpaper, Wallpaper
 * 1], then the remaining numbered slots follow two per row. */
@Composable
private fun WallpaperGrid(
    selected: Wallpaper,
    onSelect: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticTick = rememberHapticTick()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WallpaperTile(
                label = stringResource(id = R.string.appearance_wallpaper_none),
                selected = selected == Wallpaper.NONE,
                onClick = { hapticTick(); onSelect(Wallpaper.NONE) },
                modifier = Modifier.weight(1f)
            )
            WallpaperImageTile(
                label = stringResource(id = R.string.appearance_wallpaper_option_format, 1),
                selected = selected == Wallpaper.WALLPAPER_1,
                onClick = { hapticTick(); onSelect(Wallpaper.WALLPAPER_1) },
                modifier = Modifier.weight(1f)
            )
        }
        for (rowStart in 2..WALLPAPER_OPTION_COUNT step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in rowStart..minOf(rowStart + 1, WALLPAPER_OPTION_COUNT)) {
                    WallpaperTile(
                        label = stringResource(id = R.string.appearance_wallpaper_option_format, i),
                        selected = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** A placeholder Wallpaper option tile - [onClick] null (every tile but "No Wallpaper") means it
 * has no click handler at all, so it structurally cannot activate any wallpaper behavior, per
 * spec ("placeholder only, no activation logic") - reserved for a future wallpaper. */
@Composable
private fun WallpaperTile(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .let { base -> if (onClick != null) base.selectable(selected = selected, onClick = onClick) else base }
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

/** Wallpaper 1's own real, prominent thumbnail - the supplied wallpaper artwork itself (same
 * asset [com.nj031.onetask.ui.components.WallpaperBackdrop] uses), shown at its own full artwork
 * ratio rather than a special crop, per spec. Selected state = green border + a checkmark badge
 * (the spec's own words), distinct from [WallpaperTile]'s plain placeholder look. */
@Composable
private fun WallpaperImageTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .selectable(selected = selected, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.wallpaper_1),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(845f / 1860f)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
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
