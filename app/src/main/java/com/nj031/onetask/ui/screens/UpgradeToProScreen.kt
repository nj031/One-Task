package com.nj031.onetask.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R

/** One row of the Free vs Pro comparison table. Every listed Pro feature is always included, so
 * only whether it's also on Free needs to vary per row. */
private data class ProFeatureRow(val labelRes: Int, val includedInFree: Boolean)

private val proFeatureRows = listOf(
    ProFeatureRow(R.string.upgrade_to_pro_feature_timer_focus, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_local_backup, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_task_management, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_unlimited_subtasks, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_recurring_postpone, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_basic_tags, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_journal, includedInFree = true),
    ProFeatureRow(R.string.upgrade_to_pro_feature_custom_tags, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_cloud_sync, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_multi_device, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_auto_cloud_backup, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_extended_history, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_analytics, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_more_themes, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_more_fonts, includedInFree = false),
    ProFeatureRow(R.string.upgrade_to_pro_feature_task_reminders, includedInFree = false)
)

/**
 * "Upgrade to Pro" info screen reachable from the hamburger menu's Settings section. Purely
 * informational for now: the Free/Pro comparison table and the "Upgrade to Pro" button don't
 * gate or unlock anything yet - no existing Free feature is touched, and there is no real
 * purchase flow to wire up to (the intended model is a future one-time Lifetime Access
 * purchase), so the button is a non-destructive placeholder.
 */
@Composable
fun UpgradeToProScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

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
                    text = stringResource(id = R.string.upgrade_to_pro_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    ComparisonHeaderRow()
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    proFeatureRows.forEach { row ->
                        ComparisonFeatureRow(
                            label = stringResource(id = row.labelRes),
                            includedInFree = row.includedInFree
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_to_pro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.upgrade_to_pro_lifetime_access),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = stringResource(id = R.string.upgrade_to_pro_one_time_purchase),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.upgrade_to_pro_coming_soon),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_to_pro_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ComparisonHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.4f)) {}
        Text(
            text = stringResource(id = R.string.upgrade_to_pro_column_free),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.6f)
        )
        Text(
            text = stringResource(id = R.string.upgrade_to_pro_column_pro),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ComparisonFeatureRow(label: String, includedInFree: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.4f).padding(end = 8.dp)
        )
        ComparisonMark(included = includedInFree, modifier = Modifier.weight(0.6f))
        ComparisonMark(included = true, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun ComparisonMark(included: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (included) {
            stringResource(id = R.string.upgrade_to_pro_mark_included)
        } else {
            stringResource(id = R.string.upgrade_to_pro_mark_excluded)
        },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = if (included) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
