package com.nj031.onetask.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nj031.onetask.R
import com.nj031.onetask.ui.theme.OneTaskTheme

@Composable
fun AuthScreen(
    onLogIn: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(id = R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .fillMaxWidth(),
                        onClick = onLogIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(stringResource(id = R.string.log_in))
                    }

                    TextButton(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = onContinueWithGoogle
                    ) {
                        Text(
                            text = stringResource(id = R.string.continue_with_google),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = onCreateAccount,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(id = R.string.create_free_account))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenLightPreview() {
    OneTaskTheme(darkTheme = false) {
        AuthScreen(onLogIn = {}, onContinueWithGoogle = {}, onCreateAccount = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenDarkPreview() {
    OneTaskTheme(darkTheme = true) {
        AuthScreen(onLogIn = {}, onContinueWithGoogle = {}, onCreateAccount = {})
    }
}
