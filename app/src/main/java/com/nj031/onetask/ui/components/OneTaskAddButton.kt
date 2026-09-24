package com.nj031.onetask.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.data.settings.Wallpaper
import com.nj031.onetask.ui.theme.OneTaskAddIcon

private val ADD_BUTTON_SHAPE = RoundedCornerShape(28.dp)

/**
 * The Tasks screen's "Add Task" CTA - a wide, bottom-centered pill button filled with the app's
 * current primary/accent color (so it follows the user's selected Appearance color theme, unlike
 * a hardcoded color would), a white plus glyph, and its own label text.
 */
@Composable
fun OneTaskAddButton(
    onClick: () -> Unit,
    contentDescription: String,
    wallpaper: Wallpaper = Wallpaper.NONE
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp)
            .semantics { this.contentDescription = contentDescription }
            // Only while a wallpaper is active: gives this CTA a defined edge against whatever
            // wallpaper pixels happen to sit behind it - the same border token/technique Cards
            // elsewhere already use - without changing containerColor's own existing opacity.
            // Non-wallpaper themes are unaffected.
            .then(
                if (wallpaper != Wallpaper.NONE) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, ADD_BUTTON_SHAPE)
                } else {
                    Modifier
                }
            ),
        shape = ADD_BUTTON_SHAPE,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        OneTaskAddIcon(size = 20.dp, drawContainer = false, plusColor = Color.White)
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
