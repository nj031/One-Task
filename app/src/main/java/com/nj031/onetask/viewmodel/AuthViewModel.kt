package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.nj031.onetask.R
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getInstance(application).taskDao()
    private val journalNoteDao = AppDatabase.getInstance(application).journalNoteDao()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signInWithGoogle(context: Context, onSignedIn: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val idToken = requestGoogleIdToken(context)
                AuthRepository.signInWithGoogleIdToken(idToken)
                syncAfterSignIn()
                onSignedIn()
            } catch (e: GetCredentialException) {
                _errorMessage.value = "Sign-in failed: ${e::class.simpleName}: ${e.message}"
            } catch (e: GoogleIdTokenParsingException) {
                _errorMessage.value = "Couldn't verify the Google account: ${e.message}"
            } catch (e: Exception) {
                _errorMessage.value = "Sign-in failed: ${e::class.simpleName}: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun requestGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        // GetSignInWithGoogleOption (rather than GetGoogleIdOption) is the API Google's docs
        // call for when the user explicitly tapped a "Sign in with Google" button: it always
        // shows the account picker, unlike GetGoogleIdOption's filterByAuthorizedAccounts=true
        // path, which only offers accounts already authorized for this app and throws
        // NoCredentialException for a first-time signer with none yet.
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /**
     * One-time reconciliation run right after a successful sign-in: pulls any existing cloud
     * backup down into local Room (so a reinstall/new device restores prior data), then pushes
     * everything currently local up to the cloud (so anything created before signing in, or
     * only present locally, also becomes backed up). Every mutation from this point on is
     * mirrored to the cloud automatically by TaskRepository/JournalRepository.
     */
    private suspend fun syncAfterSignIn() {
        val cloudTasks = CloudBackupRepository.pullTasks()
        val cloudNotes = CloudBackupRepository.pullNotes()

        cloudTasks.forEach { taskDao.insert(it) }
        cloudNotes.forEach { journalNoteDao.insert(it) }

        CloudBackupRepository.pushAllTasks(taskDao.getAll())
        CloudBackupRepository.pushAllNotes(journalNoteDao.getAllOnce())
    }
}
