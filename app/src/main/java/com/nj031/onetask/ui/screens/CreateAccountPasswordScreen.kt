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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.viewmodel.SignUpState

/** Create an Account - Screen 2 of 4: password + confirm password, both with Show/Hide. */
@Composable
fun CreateAccountPasswordScreen(
    state: SignUpState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            AuthTopBar(title = stringResource(id = R.string.set_up_password_title), onBack = onBack)

            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(id = R.string.password_label),
                isError = state.passwordError != null,
                supportingText = state.passwordError ?: stringResource(id = R.string.password_requirement_hint),
                modifier = Modifier.padding(top = 28.dp)
            )

            PasswordField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(id = R.string.confirm_password_label),
                isError = state.passwordError != null,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (state.generalError != null) {
                Text(
                    text = state.generalError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }

            Button(
                onClick = onNext,
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
                        text = stringResource(id = R.string.next_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
