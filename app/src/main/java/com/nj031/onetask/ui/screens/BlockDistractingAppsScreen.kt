package com.nj031.onetask.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.blocking.InstalledApp
import com.nj031.onetask.ui.theme.OneTaskCardViewIcon
import com.nj031.onetask.ui.theme.OneTaskPlayIcon
import com.nj031.onetask.viewmodel.FocusBlockedAppsViewModel

/** Distinct accent colors for the YouTube/Short-form placeholder cards - arbitrary, generic
 * accent colors (not a reproduction of any app's real brand color/logo), matching this screen's
 * existing per-section color-differentiation convention (see FocusModeConfigScreen's own
 * BlockDistractionsAccent). */
private val YouTubePlaceholderAccent = Color(0xFFE53935)
private val YouTubePlaceholderAccentBackground = Color(0xFFFDEAEA)
private val ShortFormPlaceholderAccent = Color(0xFFFF7A1A)
private val ShortFormPlaceholderAccentBackground = Color(0xFFFFF1E6)
private val OtherAppsAccent = Color(0xFF22B455)
private val OtherAppsAccentBackground = Color(0xFFE8F8EE)

/**
 * Phase 5 build of the "Block distracting apps" picker, reached by tapping Focus Mode
 * Configuration's Block Distractions card - see NavGraph for the shared [FocusBlockedAppsViewModel]
 * wiring (this screen and the Configuration screen's preview both read/write the same instance,
 * scoped to Configuration's own back stack entry, so a selection made here is immediately visible
 * there and preserved if this picker is reopened).
 *
 * Structure per spec: a YouTube placeholder card and a Short-form placeholder card (both visually
 * present but inert - no controls, no blocking, Phase 7 territory), then the actual functional
 * "Other Apps" section with its two fixed categories (Distracting Apps' 9-app shortlist, and
 * Others: Discord + Telegram + every other launchable user app behind an expandable group), and a
 * "Done (N selected)" button at the end that just saves the in-memory selection and navigates back
 * - it does not start a Focus session. Selection is global by actual package identity: an app can
 * never become two separate selections just because it's reachable from two rows (it can't be -
 * every app appears in exactly one category here), and the displayed/returned count is always
 * [FocusBlockedAppsViewModel.selectedPackages]'s size, i.e. unique actual apps.
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
    var othersExpanded by remember { mutableStateOf(false) }

    val distractingPackages = remember(distractingApps) { distractingApps.map { it.packageName } }
    val othersPackages = remember(discordApp, telegramApp, otherApps) {
        listOfNotNull(discordApp?.packageName, telegramApp?.packageName) + otherApps.map { it.packageName }
    }

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

            PlaceholderMediaCard(
                icon = { tint -> OneTaskPlayIcon(tint = tint, size = 16.dp) },
                accent = YouTubePlaceholderAccent,
                accentBackground = YouTubePlaceholderAccentBackground,
                title = stringResource(id = R.string.block_apps_youtube_title),
                subtitle = stringResource(id = R.string.block_apps_youtube_subtitle),
                modifier = Modifier.padding(top = 20.dp)
            )

            PlaceholderMediaCard(
                icon = { tint -> OneTaskPlayIcon(tint = tint, size = 16.dp) },
                accent = ShortFormPlaceholderAccent,
                accentBackground = ShortFormPlaceholderAccentBackground,
                title = stringResource(id = R.string.block_apps_short_form_title),
                subtitle = stringResource(id = R.string.block_apps_short_form_subtitle),
                modifier = Modifier.padding(top = 16.dp)
            )

            FocusModeSectionLabel(
                icon = { tint -> OneTaskCardViewIcon(tint = tint, size = 18.dp) },
                iconTint = OtherAppsAccent,
                iconBackground = OtherAppsAccentBackground,
                title = stringResource(id = R.string.block_apps_other_apps_title),
                subtitle = stringResource(id = R.string.block_apps_other_apps_subtitle),
                modifier = Modifier.padding(top = 28.dp)
            )

            AppCategoryCard(
                title = stringResource(id = R.string.block_apps_distracting_apps_title),
                allSelected = distractingPackages.isNotEmpty() && distractingPackages.all { it in selectedPackages },
                onSelectAllToggle = { allSelected ->
                    viewModel.setSelected(distractingPackages, selected = !allSelected)
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                distractingApps.forEach { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onToggle = { viewModel.toggleSelected(app.packageName) },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            AppCategoryCard(
                title = stringResource(id = R.string.block_apps_others_title),
                allSelected = othersPackages.isNotEmpty() && othersPackages.all { it in selectedPackages },
                onSelectAllToggle = { allSelected ->
                    viewModel.setSelected(othersPackages, selected = !allSelected)
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                discordApp?.let { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onToggle = { viewModel.toggleSelected(app.packageName) },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                telegramApp?.let { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onToggle = { viewModel.toggleSelected(app.packageName) },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (otherApps.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { othersExpanded = !othersExpanded }
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.block_apps_all_other_user_apps),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (othersExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (othersExpanded) {
                        otherApps.forEach { app ->
                            AppRow(
                                app = app,
                                checked = app.packageName in selectedPackages,
                                onToggle = { viewModel.toggleSelected(app.packageName) },
                                modifier = Modifier.padding(top = 12.dp, start = 12.dp)
                            )
                        }
                    }
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

/** A visually-present-but-inert placeholder row (icon in a colored circle, title/subtitle, an
 * always-on switch that never changes) - same established convention as DataPrivacyScreen's own
 * ToggleRow for a control with no behavior yet. Used for both the YouTube and Short-form cards,
 * neither of which gets any real controls or blocking logic this phase. */
@Composable
private fun PlaceholderMediaCard(
    icon: @Composable (tint: Color) -> Unit,
    accent: Color,
    accentBackground: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentBackground),
                contentAlignment = Alignment.Center
            ) {
                icon(accent)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Always ON: a visual placeholder only, per Phase 5 scope - see this screen's own doc
            // comment. Actual YouTube/Short-form blocking controls are Phase 7 territory.
            Switch(checked = true, onCheckedChange = { /* not functional yet - stays ON */ })
        }
    }
}

/** One of the two Other Apps categories (Distracting Apps / Others): a bold title + a "Select
 * all"/"Deselect all" pill, followed by whatever app rows [content] renders. */
@Composable
private fun AppCategoryCard(
    title: String,
    allSelected: Boolean,
    onSelectAllToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(
                        id = if (allSelected) R.string.block_apps_deselect_all else R.string.block_apps_select_all
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSelectAllToggle(allSelected) }
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
