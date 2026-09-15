package com.nj031.onetask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nj031.onetask.R
import com.nj031.onetask.ui.components.TonalActionButton

/**
 * The single hamburger-menu drawer used by both the Tasks/Homepage and Journal screens (never a
 * separate menu design per section). Structure is always GENERAL (app-wide options) optionally
 * preceded by a quick "Journaling" nav shortcut (Tasks side only - omitted once you're already on
 * Journal), then an optional JOURNAL section (Archive/Recycle Bin), shown only when the caller is
 * Journal itself. Sections are told apart by a small label, not a heavy divider.
 */
@Composable
fun OneTaskDrawerContent(
    userEmail: String,
    onSettingsClick: () -> Unit,
    onHelpFeedbackClick: () -> Unit,
    onRateAppClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onJournalingClick: (() -> Unit)? = null,
    onArchiveClick: (() -> Unit)? = null,
    onRecycleBinClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            onJournalingClick?.let { onClick ->
                DrawerMenuRow(
                    text = stringResource(id = R.string.drawer_journaling),
                    onClick = onClick,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_general))
            DrawerMenuRow(text = stringResource(id = R.string.drawer_settings), onClick = onSettingsClick)
            DrawerMenuRow(text = stringResource(id = R.string.drawer_help_feedback), onClick = onHelpFeedbackClick)
            DrawerMenuRow(text = stringResource(id = R.string.drawer_rate_app), onClick = onRateAppClick)
            DrawerMenuRow(
                text = stringResource(id = R.string.drawer_statistics),
                onClick = { /* no-op: Premium feature, not available yet */ },
                enabled = false,
                trailing = { DrawerPremiumBadge() }
            )

            if (onArchiveClick != null && onRecycleBinClick != null) {
                DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_journal))
                DrawerMenuRow(text = stringResource(id = R.string.archive_title), onClick = onArchiveClick)
                DrawerMenuRow(text = stringResource(id = R.string.recycle_bin_title), onClick = onRecycleBinClick)
            }
        }

        Text(
            text = stringResource(id = R.string.logged_in_as),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = userEmail,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 2.dp)
        )

        TonalActionButton(
            text = stringResource(id = R.string.log_out),
            onClick = onLogoutClick,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** A plain, low-emphasis nav row - text plus an optional trailing element, no filled background,
 * so a menu full of these reads as a clean list rather than a stack of blue pills. */
@Composable
private fun DrawerMenuRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun DrawerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 18.dp, bottom = 2.dp, start = 10.dp)
    )
}

@Composable
private fun DrawerPremiumBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = R.string.drawer_premium_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
