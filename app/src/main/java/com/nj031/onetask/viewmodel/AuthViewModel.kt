package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.nj031.onetask.data.AppDatabase
import com.nj031.onetask.data.auth.AuthErrorContext
import com.nj031.onetask.data.auth.AuthErrorMessages
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.auth.AuthValidation
import com.nj031.onetask.data.sync.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null
)

data class VerifyEmailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)

data class ProfileSetupState(
    val name: String = "",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null
)

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val requestSent: Boolean = false
)

/**
 * Backs the entire authentication area (Main Login, Create Account's 4 steps, Log In, Forgot
 * Password) - hoisted once in NavGraph like HomeViewModel/ProfileViewModel, so entered form
 * values survive Back/forward navigation between these screens without ever needing to pass a
 * password through a nav route argument.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // --- Continue with Google (existing) ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** [onSignedIn] receives whether this was a brand new account with no usable Google
     * profile name - the only case that should be routed to Set Up Profile - so an existing
     * account signing back in is never sent there just because [FirebaseUser.displayName]
     * happens to be blank. */
    fun signInWithGoogle(context: Context, onSignedIn: (needsProfileSetup: Boolean) -> Unit) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val idToken = AuthRepository.requestGoogleIdToken(context)
                val result = AuthRepository.signInWithGoogleIdToken(idToken)
                syncAfterSignIn()
                val needsProfileSetup = result.isNewUser && result.user.displayName.isNullOrBlank()
                onSignedIn(needsProfileSetup)
            } catch (e: GetCredentialException) {
                _errorMessage.value = "Sign-in was cancelled or failed. Please try again."
            } catch (e: GoogleIdTokenParsingException) {
                _errorMessage.value = "Couldn't verify the Google account. Please try again."
            } catch (e: Exception) {
                _errorMessage.value = "Sign-in failed. Please try again."
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
     *
     * taskDao/journalNoteDao are deliberately looked up fresh here rather than cached as fields:
     * this ViewModel is constructed once (hoisted in NavGraph) and may already exist from before
     * this sign-in - typically while nobody was signed in yet, but a plain field captured then
     * would stay bound to that stale (pre-sign-in) per-account database connection forever. Since
     * this function runs immediately after AuthRepository's own sign-in call succeeds - and
     * before anything else reacts to the account change - resolving AppDatabase.getInstance()
     * here is what makes it see the account that JUST signed in, not whichever one (or none) was
     * active when this ViewModel was originally constructed.
     */
    private suspend fun syncAfterSignIn() {
        val application = getApplication<Application>()
        val taskDao = AppDatabase.getInstance(application).taskDao()
        val journalNoteDao = AppDatabase.getInstance(application).journalNoteDao()

        val cloudTasks = CloudBackupRepository.pullTasks()
        val cloudNotes = CloudBackupRepository.pullNotes()

        cloudTasks.forEach { taskDao.insert(it) }
        cloudNotes.forEach { journalNoteDao.insert(it) }

        CloudBackupRepository.pushAllTasks(taskDao.getAll())
        CloudBackupRepository.pushAllNotes(journalNoteDao.getAllOnce())
    }

    // --- Create Account: Screen 1 (Email) + Screen 2 (Password) ---
    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState.asStateFlow()

    fun updateSignUpEmail(email: String) {
        _signUpState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun updateSignUpPassword(password: String) {
        _signUpState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun updateSignUpConfirmPassword(confirmPassword: String) {
        _signUpState.update { it.copy(confirmPassword = confirmPassword, passwordError = null, generalError = null) }
    }

    /** Screen 1 -> Screen 2. Client-side format validation only - the email isn't checked
     * against the backend until account creation on the next screen. */
    fun submitSignUpEmail(onValid: () -> Unit) {
        val email = _signUpState.value.email.trim()
        if (!AuthValidation.isValidEmail(email)) {
            _signUpState.update { it.copy(emailError = "Please enter a valid email address.") }
            return
        }
        _signUpState.update { it.copy(email = email) }
        onValid()
    }

    /**
     * Screen 2 -> Screen 3. Validates both passwords, then creates the real Firebase account
     * and sends the verification email. If an account with this email was already created
     * earlier in this same flow (the user went Back and is retrying with a different
     * password), updates that pending account's password instead of failing with "email
     * already in use".
     */
    fun submitSignUpPassword(onSuccess: () -> Unit) {
        val state = _signUpState.value
        if (state.isLoading) return
        if (!AuthValidation.isValidPassword(state.password)) {
            _signUpState.update {
                it.copy(passwordError = "Use at least 8 characters, with at least one letter and one number.")
            }
            return
        }
        if (state.password != state.confirmPassword) {
            _signUpState.update { it.copy(passwordError = "Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _signUpState.update { it.copy(isLoading = true, generalError = null) }
            try {
                val pendingUser = AuthRepository.currentUser
                if (pendingUser != null && pendingUser.email == state.email && !pendingUser.isEmailVerified) {
                    AuthRepository.updateCurrentPassword(state.password)
                } else {
                    AuthRepository.createAccountWithEmail(state.email, state.password)
                }
                AuthRepository.sendEmailVerification()
                onSuccess()
            } catch (e: Exception) {
                _signUpState.update { it.copy(generalError = AuthErrorMessages.from(e, AuthErrorContext.SIGN_UP)) }
            } finally {
                _signUpState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Create Account: Screen 3 (Verify Email) ---
    private val _verifyEmailState = MutableStateFlow(VerifyEmailUiState())
    val verifyEmailState: StateFlow<VerifyEmailUiState> = _verifyEmailState.asStateFlow()

    fun resendVerificationEmail() {
        if (_verifyEmailState.value.isLoading) return
        viewModelScope.launch {
            _verifyEmailState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            try {
                AuthRepository.sendEmailVerification()
                _verifyEmailState.update { it.copy(infoMessage = "Verification email sent.") }
            } catch (e: Exception) {
                _verifyEmailState.update { it.copy(error = AuthErrorMessages.from(e, AuthErrorContext.SIGN_UP)) }
            } finally {
                _verifyEmailState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Reloads the account from the server and checks whether the emailed link has been used
     * yet. [onVerified] proceeds to Set Up Profile; otherwise shows an inline message. */
    fun checkEmailVerified(onVerified: () -> Unit) {
        if (_verifyEmailState.value.isLoading) return
        viewModelScope.launch {
            _verifyEmailState.update { it.copy(isLoading = true, error = null, infoMessage = null) }
            try {
                AuthRepository.reloadCurrentUser()
                if (AuthRepository.isCurrentUserEmailVerified) {
                    onVerified()
                } else {
                    _verifyEmailState.update {
                        it.copy(error = "Your email isn't verified yet. Please tap the link we sent you, then try again.")
                    }
                }
            } catch (e: Exception) {
                _verifyEmailState.update { it.copy(error = AuthErrorMessages.from(e, AuthErrorContext.SIGN_UP)) }
            } finally {
                _verifyEmailState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Create Account: Screen 4 (Set Up Profile) - also used for a Google sign-in with no name ---
    private val _profileSetupState = MutableStateFlow(ProfileSetupState())
    val profileSetupState: StateFlow<ProfileSetupState> = _profileSetupState.asStateFlow()

    fun updateProfileSetupName(name: String) {
        _profileSetupState.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    /** Finishes account creation: sets the entered name as the account's display name (which
     * ProfileViewModel/UserProfileRepository picks up automatically as the initial One Task
     * profile name the first time Profile loads, since no local profile has been saved yet -
     * no changes needed there), then runs the same post-sign-in sync Google sign-in uses. */
    fun submitProfileSetup(onDone: () -> Unit) {
        val state = _profileSetupState.value
        if (state.isLoading) return
        val name = state.name.trim()
        if (name.isBlank()) {
            _profileSetupState.update { it.copy(nameError = "Name is required.") }
            return
        }
        viewModelScope.launch {
            _profileSetupState.update { it.copy(isLoading = true, generalError = null) }
            try {
                AuthRepository.updateDisplayName(name)
                syncAfterSignIn()
                onDone()
            } catch (e: Exception) {
                _profileSetupState.update {
                    it.copy(generalError = AuthErrorMessages.from(e, AuthErrorContext.SIGN_UP))
                }
            } finally {
                _profileSetupState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Log In ---
    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun updateLoginEmail(email: String) {
        _loginState.update { it.copy(email = email, error = null) }
    }

    fun updateLoginPassword(password: String) {
        _loginState.update { it.copy(password = password, error = null) }
    }

    fun submitLogin(onSignedIn: () -> Unit) {
        val state = _loginState.value
        if (state.isLoading) return
        val email = state.email.trim()
        if (email.isBlank() || state.password.isBlank()) {
            _loginState.update { it.copy(error = "Please enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            try {
                AuthRepository.signInWithEmail(email, state.password)
                syncAfterSignIn()
                onSignedIn()
            } catch (e: Exception) {
                // Password is deliberately cleared (not the email) on failure - avoids leaving a
                // wrong/failed password sitting in the field, without losing the typed email.
                _loginState.update {
                    it.copy(password = "", error = AuthErrorMessages.from(e, AuthErrorContext.LOGIN))
                }
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Forgot Password ---
    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    fun updateForgotPasswordEmail(email: String) {
        _forgotPasswordState.update { it.copy(email = email, error = null) }
    }

    fun submitForgotPassword() {
        val state = _forgotPasswordState.value
        if (state.isLoading) return
        val email = state.email.trim()
        if (!AuthValidation.isValidEmail(email)) {
            _forgotPasswordState.update { it.copy(error = "Please enter a valid email address.") }
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.update { it.copy(isLoading = true, error = null) }
            try {
                AuthRepository.sendPasswordResetEmail(email)
                _forgotPasswordState.update { it.copy(requestSent = true) }
            } catch (e: Exception) {
                if (e is FirebaseAuthInvalidUserException) {
                    // Don't reveal whether this email has an account - show the same
                    // confirmation either way, so this can't be used to enumerate accounts.
                    _forgotPasswordState.update { it.copy(requestSent = true) }
                } else {
                    _forgotPasswordState.update {
                        it.copy(error = AuthErrorMessages.from(e, AuthErrorContext.RESET_PASSWORD))
                    }
                }
            } finally {
                _forgotPasswordState.update { it.copy(isLoading = false) }
            }
        }
    }
}
