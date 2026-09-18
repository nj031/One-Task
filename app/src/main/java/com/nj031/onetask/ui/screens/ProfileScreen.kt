package com.nj031.onetask.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.ui.components.ProfileAvatar
import com.nj031.onetask.ui.theme.OneTaskChatIcon
import com.nj031.onetask.ui.theme.OneTaskCrownIcon
import com.nj031.onetask.ui.theme.OneTaskInfoIcon
import com.nj031.onetask.ui.theme.OneTaskLogoutIcon
import com.nj031.onetask.ui.theme.OneTaskSettingsGearIcon
import com.nj031.onetask.ui.theme.OneTaskShieldIcon
import com.nj031.onetask.ui.theme.OneTaskStarIcon
import com.nj031.onetask.viewmodel.ProfileViewModel

/** One Task's application id (see app/build.gradle.kts) - the Play Store listing, once live, is
 * reached at this same id. */
private const val PLAY_STORE_PACKAGE_NAME = "com.nj031.onetask"

/** One Task isn't published on Google Play yet. Flip this to true once the app has a live Play
 * Store listing - "Rate One Task" will then open its official rating/review page as-is. */
private const val IS_PLAY_STORE_LISTING_LIVE = false

private fun openPlayStoreListing(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_STORE_PACKAGE_NAME"))
    try {
        context.startActivity(marketIntent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE_NAME")
            )
        )
    }
}

/**
 * The unified Profile & Settings screen - the single destination for everything that used to be
 * split between the Profile tab and the hamburger drawer (Account/Journal/Settings/About). It's
 * reached by tapping the avatar icon in the Tasks/Notes top bars, and behaves like any other
 * pushed settings screen (back arrow, no bottom nav), not like the old Profile bottom-nav tab.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onUpgradeToProClick: () -> Unit = {},
    onGeneralSettingsClick: () -> Unit = {},
    onDataPrivacyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onHelpFeedbackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(id = R.string.profile_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(id = R.string.profile_settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(photoPath = profile.photoPath, size = 84.dp)
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = viewModel.userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onEditProfileClick)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.profile_edit_profile),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            ProfileSectionLabel(text = stringResource(id = R.string.profile_section_plan), topPadding = 28.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OneTaskCrownIcon(tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                        Text(
                            text = stringResource(id = R.string.profile_pro_free_plan),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.profile_pro_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = onUpgradeToProClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.drawer_upgrade_to_pro))
                    }
                }
            }

            ProfileSectionLabel(text = stringResource(id = R.string.profile_section_settings), topPadding = 24.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileMenuRow(
                        icon = { tint -> OneTaskSettingsGearIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.drawer_settings_general),
                        onClick = onGeneralSettingsClick
                    )
                    ProfileMenuRow(
                        icon = { tint -> OneTaskShieldIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.drawer_data_privacy),
                        onClick = onDataPrivacyClick
                    )
                }
            }

            ProfileSectionLabel(text = stringResource(id = R.string.profile_section_about), topPadding = 24.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileMenuRow(
                        icon = { tint -> OneTaskInfoIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.drawer_about_one_task),
                        onClick = onAboutClick
                    )
                    ProfileMenuRow(
                        icon = { tint -> OneTaskChatIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.drawer_help_feedback),
                        onClick = onHelpFeedbackClick
                    )
                    ProfileMenuRow(
                        icon = { tint -> OneTaskStarIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.drawer_rate_app),
                        onClick = { showRateDialog = true }
                    )
                }
            }

            ProfileSectionLabel(text = stringResource(id = R.string.profile_section_account), topPadding = 24.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileMenuRow(
                        icon = { tint -> OneTaskLogoutIcon(tint = tint, size = 22.dp) },
                        text = stringResource(id = R.string.log_out),
                        onClick = { showLogoutConfirm = true },
                        showChevron = false
                    )
                }
            }
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
                        onLogout()
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

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text(text = stringResource(id = R.string.rate_app_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.rate_app_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRateDialog = false
                        if (IS_PLAY_STORE_LISTING_LIVE) {
                            openPlayStoreListing(context)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.rate_app_not_available_message),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.drawer_rate_app))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text(text = stringResource(id = R.string.rate_app_maybe_later))
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: @Composable (tint: Color) -> Unit,
    text: String,
    onClick: () -> Unit,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding, bottom = 8.dp, start = 4.dp)
    )
}
