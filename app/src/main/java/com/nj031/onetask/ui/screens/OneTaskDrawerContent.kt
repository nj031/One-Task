package com.nj031.onetask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * The single global hamburger-menu drawer used by every screen - identical structure and
 * content everywhere it's opened from (Tasks, Journal, ...), never a per-screen variant.
 * Structure: branding, then an ACCOUNT card, a JOURNAL card (Archive/Recycle Bin), then plain
 * SETTINGS and ABOUT row sections. Cards carry the app's white surface color to stand out
 * against the drawer's own light background; plain rows sit directly on that background.
 */
@Composable
fun OneTaskDrawerContent(
    userEmail: String,
    onLogoutClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onGeneralSettingsClick: () -> Unit,
    onDataPrivacyClick: () -> Unit,
    onUpgradeToProClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpFeedbackClick: () -> Unit,
    onRateAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
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

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.home_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_account))
            DrawerCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
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
                }
                DrawerMenuRow(
                    text = stringResource(id = R.string.log_out),
                    onClick = { showLogoutConfirm = true }
                )
            }

            DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_journal))
            DrawerCard {
                DrawerMenuRow(text = stringResource(id = R.string.archive_title), onClick = onArchiveClick)
                DrawerMenuRow(text = stringResource(id = R.string.recycle_bin_title), onClick = onRecycleBinClick)
            }

            DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_settings))
            DrawerMenuRow(text = stringResource(id = R.string.drawer_settings_general), onClick = onGeneralSettingsClick)
            DrawerMenuRow(text = stringResource(id = R.string.drawer_data_privacy), onClick = onDataPrivacyClick)
            DrawerMenuRow(
                text = stringResource(id = R.string.drawer_upgrade_to_pro),
                onClick = onUpgradeToProClick,
                emphasized = true
            )

            DrawerSectionLabel(text = stringResource(id = R.string.drawer_section_about))
            DrawerMenuRow(text = stringResource(id = R.string.drawer_about_one_task), onClick = onAboutClick)
            DrawerMenuRow(text = stringResource(id = R.string.drawer_help_feedback), onClick = onHelpFeedbackClick)
            DrawerMenuRow(text = stringResource(id = R.string.drawer_rate_app), onClick = onRateAppClick)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(text = stringResource(id = R.string.logout_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogoutClick()
                    }
                ) {
                    Text(text = stringResource(id = R.string.logout_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

/** A white, rounded surface grouping a handful of closely related rows (Account, Journal) so
 * they read as one prominent unit against the drawer's own light background. */
@Composable
private fun DrawerCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

/** A plain, low-emphasis nav row - text only, no filled background, so a menu full of these
 * reads as a clean list rather than a stack of blue pills. [emphasized] gives the "Upgrade to
 * Pro" row a premium look (bold, primary-colored) without turning it into its own card. */
@Composable
private fun DrawerMenuRow(text: String, onClick: () -> Unit, emphasized: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 10.dp)
    )
}
