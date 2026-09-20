package com.nj031.onetask.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.theme.OneTaskWallpapers

/**
 * The wallpaper image + its dark overlay scrim, drawn full-bleed behind a screen's own content -
 * see the Wallpaper spec's "image scope" rule: only the Task, Timer, and Notes screens call this;
 * every other screen (Settings, Appearance, Add/Edit Task, ...) only ever picks up the active
 * wallpaper's color PALETTE (see [com.nj031.onetask.ui.theme.OneTaskTheme]), never this image. A
 * total no-op - renders nothing at all - for [Wallpaper.NONE], so a screen that calls this
 * unconditionally looks and behaves exactly as it did before wallpapers existed whenever no
 * wallpaper is selected.
 *
 * [ContentScale.Crop] (scale-to-fill, then center-crop the overflow) is used rather than Fit/
 * FillBounds so the image always fully covers the screen on any phone or tablet aspect ratio with
 * no blank margin and no stretching/distortion - the trade-off, an ordinary and visually
 * unsurprising one for a portrait-composed background image, is that a screen whose aspect ratio
 * differs a lot from the source artwork's own (e.g. a tablet) crops some of the top/bottom (or
 * left/right) of the image rather than showing all of it letterboxed.
 */
@Composable
fun WallpaperBackdrop(wallpaper: Wallpaper, darkTheme: Boolean, modifier: Modifier = Modifier) {
    val definition = OneTaskWallpapers.definitionFor(wallpaper) ?: return
    val imageRes = if (darkTheme) definition.darkImageRes else definition.lightImageRes
    val overlay = if (darkTheme) definition.dark.overlay else definition.light.overlay

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Box(modifier = modifier.fillMaxSize().background(overlay))
}
