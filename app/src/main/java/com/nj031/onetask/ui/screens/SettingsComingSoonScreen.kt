package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R

/**
 * Shared "not built yet" screen for a General Settings row whose real UI hasn't been designed
 * yet - same treatment Privacy Policy already got before its content existed. Used for
 * Appearance (explicitly a placeholder per spec) and for the General rows this pass doesn't
 * implement (Default Task Settings, Notifications, Week Starts On, Time Format, Haptic
 * Feedback), each with its own title but the same generic message.
 */
@Composable
fun SettingsComingSoonScreen(title: String, message: String, onBackClick: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
