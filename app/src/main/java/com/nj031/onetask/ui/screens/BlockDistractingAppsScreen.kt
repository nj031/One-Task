package com.nj031.onetask.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.blocking.InstalledApp
import com.nj031.onetask.viewmodel.FocusBlockedAppsViewModel

/**
 * The "Block distracting apps" picker, reached by tapping Focus Mode Configuration's Block
 * Distractions card - see NavGraph for the shared [FocusBlockedAppsViewModel] wiring (this screen
 * and the Configuration screen's preview both read/write the same instance, scoped to
 * Configuration's own back stack entry, so a selection made here is immediately visible there and
 * preserved if this picker is reopened).
 *
 * A single alphabetical (by displayed label) list of every eligible app - the Distracting Apps
 * shortlist, Discord, Telegram, and every other launchable user app are all merged into one list
 * here rather than shown as separate categories; [FocusBlockedAppsViewModel] itself still loads
 * them as four separate lists (unchanged - FocusModeConfigScreen's own preview still reads those
 * directly), this screen just combines and sorts them for display. A single "Select all"/
 * "Deselect all" action at the top acts on the whole merged list, and a "Done (N selected)" button
 * at the end just saves the in-memory selection and navigates back - it does not start a Focus
 * session. Selection is global by actual package identity: an app can never become two separate
 * selections just because it's reachable from two of the underlying lists (it can't be - every
 * app appears in exactly one of them), and the displayed/returned count is always
 * [FocusBlockedAppsViewModel.selectedPackages]'s size, i.e. unique actual apps.
 *
 * No YouTube- or short-form-specific card/controls exist here: neither ever had real blocking
 * behavior, and both were removed rather than kept as inert placeholders. YouTube itself remains
 * selectable exactly like any other installed user app - it already surfaces via the same
 * InstalledAppsRepository query every other non-shortlisted app goes through, with no
 * special-casing needed or present.
 */
@Composable
fun BlockDistractingAppsScreen(
    viewModel: FocusBlockedAppsViewModel = viewModel(),
    onDone: () -> Unit
) {
    val distractingApps by viewModel.distractingApps.collectAsState()
    val discordApp by viewModel.discordApp.collectAsState()
    val telegramApp by viewModel.telegramApp.collectAsState()
    val otherApps by viewModel.otherApps.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()

    val allApps = remember(distractingApps, discordApp, telegramApp, otherApps) {
        (distractingApps + listOfNotNull(discordApp, telegramApp) + otherApps)
            .sortedBy { it.label.lowercase() }
    }
    val allPackages = remember(allApps) { allApps.map { it.packageName } }
    val allSelected = allPackages.isNotEmpty() && allPackages.all { it in selectedPackages }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(id = R.string.focus_mode_config_block_distractions_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.block_apps_screen_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )

            AppListCard(
                allSelected = allSelected,
                onSelectAllToggle = { viewModel.setSelected(allPackages, selected = !allSelected) },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                allApps.forEach { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onToggle = { viewModel.toggleSelected(app.packageName) },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                Text(
                    text = stringResource(id = R.string.block_apps_done_format, selectedPackages.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** The single app list card: a "Select all"/"Deselect all" action at the top, followed by
 * whatever app rows [content] renders - no per-category title, since the picker is now one
 * continuous alphabetical list rather than separate categories. */
@Composable
private fun AppListCard(
    allSelected: Boolean,
    onSelectAllToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        id = if (allSelected) R.string.block_apps_deselect_all else R.string.block_apps_select_all
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSelectAllToggle() }
                )
            }
            content()
        }
    }
}

/** One selectable app row: real launcher icon (see [AppIconImage]) + name + a Material3
 * checkbox - never a package name, per spec. The whole row (not just the checkbox) toggles
 * selection, matching this app's other tappable-row conventions. */
@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconImage(app = app, size = 36.dp)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

/** Renders an [InstalledApp]'s own real launcher icon (from PackageManager, never a bundled
 * redrawn copy) as a small rounded tile - shared with FocusModeConfigScreen's Block Distractions
 * preview row so both screens render the exact same apps identically. */
@Composable
fun AppIconImage(app: InstalledApp, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(
            width = app.icon.intrinsicWidth.coerceAtLeast(1),
            height = app.icon.intrinsicHeight.coerceAtLeast(1)
        ).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
    )
}
