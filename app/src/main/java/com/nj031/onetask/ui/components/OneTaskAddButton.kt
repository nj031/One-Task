package com.nj031.onetask.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nj031.onetask.ui.theme.OneTaskAddIcon

private val AddButtonBlue = Color(0xFF2F6FD6)

/**
 * The Homepage's "Add (+)" floating action button (add task) - circular, solid blue, white
 * plus glyph.
 */
@Composable
fun OneTaskAddButton(onClick: () -> Unit, contentDescription: String) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        containerColor = AddButtonBlue,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        OneTaskAddIcon(size = 28.dp, drawContainer = false, plusColor = Color.White)
    }
}
