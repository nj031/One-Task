package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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
                val idToken = AuthRepository.requestGoogleIdToken(context)
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
