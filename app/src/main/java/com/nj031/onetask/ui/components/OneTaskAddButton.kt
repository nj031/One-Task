package com.nj031.onetask.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.theme.OneTaskAddIcon

/**
 * The Tasks screen's "Add (+)" floating action button - circular, filled with the app's current
 * primary/accent color (so it follows the user's selected Appearance color theme, unlike a
 * hardcoded color would), white plus glyph.
 */
@Composable
fun OneTaskAddButton(
    onClick: () -> Unit,
    contentDescription: String,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            // Only while a wallpaper is active: gives this CTA a defined edge against whatever
            // wallpaper pixels happen to sit behind it - the same border token/technique Cards
            // elsewhere already use - without changing containerColor's own existing opacity.
            // Non-wallpaper themes are unaffected.
            .then(
                if (wallpaper != Wallpaper.NONE) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                } else {
                    Modifier
                }
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        OneTaskAddIcon(size = 28.dp, drawContainer = false, plusColor = Color.White)
    }
}
