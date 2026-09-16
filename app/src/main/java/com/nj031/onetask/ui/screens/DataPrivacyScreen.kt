package com.nj031.onetask.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.ui.haptics.rememberHapticTick
import com.nj031.onetask.viewmodel.DataPrivacyViewModel
import kotlinx.coroutines.launch

private const val DELETE_STAGE_NONE = 0
private const val DELETE_STAGE_FIRST = 1
private const val DELETE_STAGE_FINAL = 2

/**
 * Settings > Data & Privacy. Cloud Sync / Automatic Backup are visual-only for now (always ON,
 * taps are no-ops) - the app's existing per-write cloud mirroring (CloudBackupRepository, wired
 * into TaskRepository/JournalRepository) keeps running exactly as before regardless of this
 * screen. Export/Restore/Delete All Data operate on that same Task/Journal data; Delete Account
 * additionally removes the Firebase Auth account itself.
 */
@Composable
fun DataPrivacyScreen(
    viewModel: DataPrivacyViewModel = viewModel(),
    onBackClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBusy by remember { mutableStateOf(false) }
    val hapticTick = rememberHapticTick()

    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var deleteAllStage by remember { mutableStateOf(DELETE_STAGE_NONE) }
    var deleteAccountStage by remember { mutableStateOf(DELETE_STAGE_NONE) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            try {
                val json = viewModel.exportDataJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(context, context.getString(R.string.data_privacy_export_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.data_privacy_export_failed, e.message ?: e.toString()),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isBusy = false
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) restoreUri = uri }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(id = R.string.data_privacy_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            SectionLabel(text = stringResource(id = R.string.data_privacy_section_backup_sync), topPadding = 24.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ToggleRow(
                        title = stringResource(id = R.string.data_privacy_cloud_sync),
                        description = stringResource(id = R.string.data_privacy_cloud_sync_description)
                    )
                    ToggleRow(
                        title = stringResource(id = R.string.data_privacy_automatic_backup),
                        description = stringResource(id = R.string.data_privacy_automatic_backup_description)
                    )
                }
            }

            NavRow(
                text = stringResource(id = R.string.data_privacy_export_data),
                modifier = Modifier.padding(top = 20.dp),
                onClick = { exportLauncher.launch("one_task_backup.json") }
            )
            NavRow(
                text = stringResource(id = R.string.data_privacy_restore_data),
                onClick = {
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }
            )
            NavRow(
                text = stringResource(id = R.string.data_privacy_delete_all_data),
                destructive = true,
                onClick = { deleteAllStage = DELETE_STAGE_FIRST }
            )
            NavRow(
                text = stringResource(id = R.string.data_privacy_delete_account),
                destructive = true,
                onClick = { deleteAccountStage = DELETE_STAGE_FIRST }
            )
            NavRow(
                text = stringResource(id = R.string.data_privacy_privacy_policy),
                onClick = onPrivacyPolicyClick
            )

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
            }
        }
    }

    if (restoreUri != null) {
        val uriToRestore = restoreUri
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(text = stringResource(id = R.string.data_privacy_restore_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.data_privacy_restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreUri = null
                        scope.launch {
                            isBusy = true
                            try {
                                val json = uriToRestore?.let { uri ->
                                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                } ?: error("Couldn't read the selected file")
                                viewModel.restoreDataJson(json)
                                Toast.makeText(context, context.getString(R.string.data_privacy_restore_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.data_privacy_restore_failed, e.message ?: e.toString()),
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.data_privacy_restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreUri = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (deleteAllStage == DELETE_STAGE_FIRST) {
        AlertDialog(
            onDismissRequest = { deleteAllStage = DELETE_STAGE_NONE },
            title = { Text(text = stringResource(id = R.string.data_privacy_delete_all_data)) },
            text = { Text(text = stringResource(id = R.string.data_privacy_delete_all_data_message)) },
            confirmButton = {
                TextButton(onClick = { deleteAllStage = DELETE_STAGE_FINAL }) {
                    Text(text = stringResource(id = R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllStage = DELETE_STAGE_NONE }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
    if (deleteAllStage == DELETE_STAGE_FINAL) {
        AlertDialog(
            onDismissRequest = { deleteAllStage = DELETE_STAGE_NONE },
            title = { Text(text = stringResource(id = R.string.data_privacy_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.data_privacy_delete_data_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticTick()
                        deleteAllStage = DELETE_STAGE_NONE
                        scope.launch {
                            isBusy = true
                            try {
                                viewModel.deleteAllData()
                                Toast.makeText(context, context.getString(R.string.data_privacy_delete_all_data_success), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: e.toString(), Toast.LENGTH_LONG).show()
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.data_privacy_delete_data_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllStage = DELETE_STAGE_NONE }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (deleteAccountStage == DELETE_STAGE_FIRST) {
        AlertDialog(
            onDismissRequest = { deleteAccountStage = DELETE_STAGE_NONE },
            title = { Text(text = stringResource(id = R.string.data_privacy_delete_account_title)) },
            text = { Text(text = stringResource(id = R.string.data_privacy_delete_account_message)) },
            confirmButton = {
                TextButton(onClick = { deleteAccountStage = DELETE_STAGE_FINAL }) {
                    Text(text = stringResource(id = R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAccountStage = DELETE_STAGE_NONE }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
    if (deleteAccountStage == DELETE_STAGE_FINAL) {
        AlertDialog(
            onDismissRequest = { deleteAccountStage = DELETE_STAGE_NONE },
            title = { Text(text = stringResource(id = R.string.data_privacy_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.data_privacy_delete_account_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticTick()
                        deleteAccountStage = DELETE_STAGE_NONE
                        scope.launch {
                            isBusy = true
                            try {
                                viewModel.deleteAccount(context)
                                onAccountDeleted()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.data_privacy_delete_account_failed, e.message ?: e.toString()),
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.data_privacy_delete_account_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAccountStage = DELETE_STAGE_NONE }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ToggleRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        // Always ON: this control is a visual placeholder only, per current product scope -
        // the app's real cloud sync/backup already runs unconditionally on every write and
        // must keep doing so regardless of what's tapped here.
        Switch(checked = true, onCheckedChange = { /* not functional yet - stays ON */ })
    }
}

@Composable
private fun NavRow(
    text: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding, bottom = 6.dp, start = 4.dp)
    )
}
