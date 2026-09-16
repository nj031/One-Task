package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.feedback.FeedbackType
import com.nj031.onetask.viewmodel.FeedbackFormViewModel
import com.nj031.onetask.viewmodel.FeedbackSubmitState

private const val MAX_MESSAGE_LENGTH = 2000

/**
 * Shared screen for Suggest a Feature / Report a Problem / Send Feedback - identical layout and
 * submit/loading/retry behavior for all three, differing only in [type]'s copy (title,
 * description, field label, success message) and, for bug reports, the device diagnostics
 * FeedbackFormViewModel automatically attaches. Entered text lives in this composable's own
 * rememberSaveable state (not the ViewModel), so it survives rotation and is never cleared on a
 * failed submission - only a confirmed successful submission (uiState becomes Success) replaces
 * the form with the thank-you view.
 */
@Composable
fun FeedbackFormScreen(
    type: FeedbackType,
    viewModel: FeedbackFormViewModel = viewModel(),
    onBackClick: () -> Unit,
    onDone: () -> Unit
) {
    var message by rememberSaveable { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val isSubmitting = uiState is FeedbackSubmitState.Submitting

    val titleRes = when (type) {
        FeedbackType.FEATURE -> R.string.suggest_feature_title
        FeedbackType.BUG -> R.string.report_problem_title
        FeedbackType.FEEDBACK -> R.string.send_feedback_title
    }
    val descriptionRes = when (type) {
        FeedbackType.FEATURE -> R.string.suggest_feature_description
        FeedbackType.BUG -> R.string.report_problem_description
        FeedbackType.FEEDBACK -> R.string.send_feedback_description
    }
    val fieldLabelRes = when (type) {
        FeedbackType.FEATURE -> R.string.suggest_feature_field_label
        FeedbackType.BUG -> R.string.report_problem_field_label
        FeedbackType.FEEDBACK -> R.string.send_feedback_field_label
    }
    val successMessageRes = when (type) {
        FeedbackType.FEATURE -> R.string.suggest_feature_success_message
        FeedbackType.BUG -> R.string.report_problem_success_message
        FeedbackType.FEEDBACK -> R.string.send_feedback_success_message
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        if (uiState is FeedbackSubmitState.Success) {
            FeedbackSuccessContent(
                modifier = Modifier.padding(innerPadding),
                successMessageRes = successMessageRes,
                onDone = onDone
            )
        } else {
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
                        text = stringResource(id = titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Text(
                    text = stringResource(id = descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) message = it },
                    label = { Text(text = stringResource(id = fieldLabelRes)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    minLines = 6,
                    enabled = !isSubmitting
                )
                Text(
                    text = stringResource(id = R.string.feedback_form_char_count, message.length, MAX_MESSAGE_LENGTH),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                val errorState = uiState as? FeedbackSubmitState.Error
                if (errorState != null) {
                    Text(
                        text = errorState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.submit(type, message.trim()) },
                    enabled = message.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.feedback_form_submit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSuccessContent(modifier: Modifier = Modifier, successMessageRes: Int, onDone: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.feedback_form_success_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = successMessageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.feedback_form_done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
