package com.nj031.onetask.ui.components

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
import com.nj031.onetask.ui.theme.OneTaskAddIcon

/**
 * The Tasks screen's "Add (+)" floating action button - circular, filled with the app's current
 * primary/accent color (so it follows the user's selected Appearance color theme, unlike a
 * hardcoded color would), white plus glyph.
 */
@Composable
fun OneTaskAddButton(onClick: () -> Unit, contentDescription: String) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        OneTaskAddIcon(size = 28.dp, drawContainer = false, plusColor = Color.White)
    }
}
