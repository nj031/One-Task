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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R

/** One Task's official support inbox - pre-filled as the "To" address when the user taps
 * Contact Support. This is the only Help & Feedback action that uses email; the other three
 * (Suggest a Feature/Report a Problem/Send Feedback) submit to the backend directly. */
private const val SUPPORT_EMAIL = "hello.onetask@gmail.com"

/**
 * Help & Feedback landing screen, reachable from the hamburger menu. Four of its five rows
 * navigate to a dedicated screen (Help & FAQ placeholder, or one of the three backend-backed
 * feedback forms); Contact Support instead launches the device's email composer directly,
 * since it's the one action here that's explicitly required to stay email-based.
 */
@Composable
fun HelpFeedbackScreen(
    onBackClick: () -> Unit,
    onHelpFaqClick: () -> Unit,
    onSuggestFeatureClick: () -> Unit,
    onReportProblemClick: () -> Unit,
    onSendFeedbackClick: () -> Unit
) {
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
                    text = stringResource(id = R.string.drawer_help_feedback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            HelpFeedbackRow(
                title = stringResource(id = R.string.help_feedback_faq_title),
                subtitle = stringResource(id = R.string.help_feedback_faq_subtitle),
                modifier = Modifier.padding(top = 20.dp),
                onClick = onHelpFaqClick
            )
            HelpFeedbackRow(
                title = stringResource(id = R.string.help_feedback_suggest_title),
                subtitle = stringResource(id = R.string.help_feedback_suggest_subtitle),
                onClick = onSuggestFeatureClick
            )
            HelpFeedbackRow(
                title = stringResource(id = R.string.help_feedback_report_title),
                subtitle = stringResource(id = R.string.help_feedback_report_subtitle),
                onClick = onReportProblemClick
            )
            HelpFeedbackRow(
                title = stringResource(id = R.string.help_feedback_send_title),
                subtitle = stringResource(id = R.string.help_feedback_send_subtitle),
                onClick = onSendFeedbackClick
            )
            HelpFeedbackRow(
                title = stringResource(id = R.string.help_feedback_contact_title),
                subtitle = stringResource(id = R.string.help_feedback_contact_subtitle),
                onClick = { openSupportEmail(context) }
            )
        }
    }
}

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.help_feedback_contact_subject))
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.help_feedback_no_email_app), Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun HelpFeedbackRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
