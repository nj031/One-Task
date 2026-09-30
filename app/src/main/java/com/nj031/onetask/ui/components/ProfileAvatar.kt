package com.nj031.onetask.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.nj031.onetask.R
import com.nj031.onetask.data.settings.Wallpaper

/**
 * The user's profile photo, or the default person glyph when none is set - shared by every
 * place the app shows the user's avatar (the Profile & Settings entry icon on Tasks/Notes, and
 * the Profile & Settings screen's own profile card) so a photo update is reflected identically,
 * and immediately, everywhere it's used, since all of them read the same [ProfileViewModel]
 * StateFlow value. While Wallpaper 1 is active and no photo is set, the fallback glyph is its
 * own supplied final asset (R.drawable.ic_wp1_profile, shown exactly as given, no tinting)
 * instead of the generic Material person icon - a real photo always takes priority regardless
 * of theme.
 */
@Composable
internal fun ProfileAvatar(
    photoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    val bitmap = remember(photoPath) {
        photoPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else if (wallpaper == Wallpaper.WALLPAPER_1) {
            Image(
                painter = painterResource(id = R.drawable.ic_wp1_profile),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
