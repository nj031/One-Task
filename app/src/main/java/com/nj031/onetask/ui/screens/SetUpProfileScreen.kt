package com.nj031.onetask.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.viewmodel.ProfileSetupState

/**
 * Create an Account - Screen 4 of 4 (also reused for a Google sign-in that didn't supply a
 * usable display name). Only collects Name - DOB/Gender are deliberately never asked here,
 * they're Profile-only and can be added later via Edit Profile.
 */
@Composable
fun SetUpProfileScreen(
    state: ProfileSetupState,
    onNameChange: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            AuthTopBar(title = stringResource(id = R.string.set_up_profile_title), onBack = onBack)

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(text = stringResource(id = R.string.profile_edit_name_label)) },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = { if (state.nameError != null) Text(text = state.nameError) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            )

            Text(
                text = stringResource(id = R.string.set_up_profile_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (state.generalError != null) {
                Text(
                    text = state.generalError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }

            Button(
                onClick = onFinish,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.finish_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
