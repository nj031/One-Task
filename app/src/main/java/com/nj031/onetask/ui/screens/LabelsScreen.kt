package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.viewmodel.JournalViewModel

/**
 * Notes > (3-dot menu) > Labels - manages the set of labels available to assign to notes,
 * conceptually the Notes equivalent of Default Task Settings' custom tags list, minus deletion
 * (not part of this feature yet). Assigning a label to an individual note isn't part of this
 * screen; this only creates/lists the labels themselves.
 */
@Composable
fun LabelsScreen(
    viewModel: JournalViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val labels by viewModel.labels.collectAsState()
    var newLabelName by remember { mutableStateOf("") }
    val trimmedName = newLabelName.trim()
    val isDuplicate = trimmedName.isNotEmpty() && labels.any { it.equals(trimmedName, ignoreCase = true) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                LabelsTopBar(onBackClick = onBackClick)

                TextField(
                    value = newLabelName,
                    onValueChange = { newLabelName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.labels_name_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    isError = isDuplicate,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (labels.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.no_labels_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        labels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (trimmedName.isNotEmpty() && !isDuplicate) {
                            viewModel.addLabel(trimmedName)
                            newLabelName = ""
                        }
                    },
                    enabled = trimmedName.isNotEmpty() && !isDuplicate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Text(
                        text = stringResource(id = R.string.labels_add_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelsTopBar(onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = stringResource(id = R.string.labels_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
