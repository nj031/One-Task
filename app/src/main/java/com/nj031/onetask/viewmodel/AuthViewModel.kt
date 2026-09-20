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
import com.nj031.onetask.data.journal.NoteLabelEntity
import com.nj031.onetask.data.profile.Gender
import com.nj031.onetask.data.profile.ProfilePhotoStorage
import com.nj031.onetask.data.profile.UserProfileRepository
import com.nj031.onetask.data.settings.AppearanceSettingsRepository
import com.nj031.onetask.data.settings.ColorTheme
import com.nj031.onetask.data.settings.DisplayMode
import com.nj031.onetask.data.settings.GeneralSettingsRepository
import com.nj031.onetask.data.settings.GeneralSettingsSnapshot
import com.nj031.onetask.data.settings.NotesViewMode
import com.nj031.onetask.data.settings.StartScreen
import com.nj031.onetask.data.settings.TimeFormat
import com.nj031.onetask.data.sync.CloudAppearance
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.data.sync.CloudGeneralSettings
import com.nj031.onetask.data.sync.CloudProfile
import com.nj031.onetask.data.sync.SyncDecision
import com.nj031.onetask.data.sync.decideSync
import com.nj031.onetask.data.task.TaskTagEntity
import java.time.DayOfWeek
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
     * backup down into local storage (Room for Tasks/Notes/Tags/Labels, SharedPreferences for
     * Appearance/General Settings/Profile, a local file for the profile photo) - so a reinstall
     * or a first sign-in on a new device restores everything this account owns - then pushes
     * anything currently local-only up to the cloud. Every mutation from this point on is
     * mirrored to the cloud automatically by the relevant repository/ViewModel.
     *
     * Each category below (Tasks/Notes, Tags, Labels, Appearance, General Settings, Profile) is
     * isolated in its own try/catch: a failure pulling/pushing one (a transient network error, a
     * permission issue) must never prevent the others from restoring, and must never leave this
     * function throwing all the way out to the sign-in screen's generic "Sign-in failed" handler
     * - Firebase Auth has already genuinely succeeded by the time this runs, and a sync hiccup
     * here shouldn't contradict that. A failed PULL for any one category always leaves that
     * category's local state untouched rather than proceeding to push - never treating "the pull
     * failed" the same as "the cloud has nothing", which would risk pushing a stale/incomplete
     * local copy over a cloud copy this function was never actually able to see.
     *
     * DAOs/repositories are deliberately looked up fresh here rather than cached as fields: this
     * ViewModel is constructed once (hoisted in NavGraph) and may already exist from before this
     * sign-in - typically while nobody was signed in yet, but a plain field captured then would
     * stay bound to that stale (pre-sign-in) per-account database/prefs connection forever. Since
     * this function runs immediately after AuthRepository's own sign-in call succeeds - and
     * before anything else reacts to the account change - resolving AppDatabase.getInstance()/
     * UserScopedPreferences here is what makes every one of them see the account that JUST
     * signed in, not whichever one (or none) was active when this ViewModel was originally
     * constructed.
     */
    private suspend fun syncAfterSignIn() {
        val application = getApplication<Application>()
        val taskDao = AppDatabase.getInstance(application).taskDao()
        val journalNoteDao = AppDatabase.getInstance(application).journalNoteDao()

        try {
            val cloudTasks = CloudBackupRepository.pullTasks()
            val cloudNotes = CloudBackupRepository.pullNotes()
            cloudTasks.forEach { taskDao.insert(it) }
            cloudNotes.forEach { journalNoteDao.insert(it) }
            CloudBackupRepository.pushAllTasks(taskDao.getAll())
            CloudBackupRepository.pushAllNotes(journalNoteDao.getAllOnce())
        } catch (_: Exception) {
            // Local Tasks/Notes are left exactly as they were - never partially merged.
        }

        try {
            // See RecurringExclusionEntity - without restoring these too, a recurring
            // occurrence the user deleted on another device (or before a reinstall) could
            // reappear here once its still-active series syncs back in above.
            val cloudExclusions = CloudBackupRepository.pullRecurringExclusions()
            cloudExclusions.forEach { taskDao.insertRecurringExclusion(it) }
            CloudBackupRepository.pushAllRecurringExclusions(taskDao.getAllRecurringExclusions())
        } catch (_: Exception) {
            // Local recurring-occurrence exclusions left untouched.
        }

        try {
            val cloudTags = CloudBackupRepository.pullTags()
            // IGNORE (not REPLACE): a tag's name is its entire identity/content, so there's
            // never anything to merge for one already present locally - this only ever adds
            // tags this account created on another device that aren't here yet.
            cloudTags.forEach { taskDao.insertTag(TaskTagEntity(name = it)) }
            CloudBackupRepository.pushAllTags(taskDao.getCustomTagsOnce())
        } catch (_: Exception) {
            // Local Custom Tags left untouched.
        }

        try {
            val cloudCategories = CloudBackupRepository.pullCategories()
            // REPLACE (not IGNORE, unlike insertTag) - a category's id is stable but its name can
            // change (see TaskRepository.renameCustomCategory), so a category already present
            // locally must still pick up a rename that happened on another device.
            cloudCategories.forEach { taskDao.insertCategory(it) }
            CloudBackupRepository.pushAllCategories(taskDao.getCustomCategoriesOnce())
        } catch (_: Exception) {
            // Local Custom Categories left untouched.
        }

        try {
            val cloudLabels = CloudBackupRepository.pullLabels()
            cloudLabels.forEach { journalNoteDao.insertLabel(NoteLabelEntity(name = it)) }
            CloudBackupRepository.pushAllLabels(journalNoteDao.getLabelsOnce())
        } catch (_: Exception) {
            // Local Note Labels left untouched.
        }

        try {
            val appearanceRepo = AppearanceSettingsRepository(application)
            val localAppearance = appearanceRepo.getSnapshot()
            val cloudAppearance = CloudBackupRepository.pullAppearance()
            when (decideSync(localAppearance.updatedAt, cloudAppearance?.updatedAt)) {
                SyncDecision.APPLY_REMOTE -> {
                    checkNotNull(cloudAppearance)
                    appearanceRepo.applyRemote(
                        displayMode = runCatching { DisplayMode.valueOf(cloudAppearance.displayMode) }.getOrDefault(DisplayMode.SYSTEM),
                        colorTheme = runCatching { ColorTheme.valueOf(cloudAppearance.colorTheme) }.getOrDefault(ColorTheme.BLUE),
                        updatedAt = cloudAppearance.updatedAt
                    )
                }
                SyncDecision.PUSH_LOCAL -> CloudBackupRepository.pushAppearance(
                    CloudAppearance(localAppearance.displayMode.name, localAppearance.colorTheme.name, localAppearance.updatedAt)
                )
            }
        } catch (_: Exception) {
            // Local Appearance left untouched.
        }

        try {
            val settingsRepo = GeneralSettingsRepository(application)
            val local = settingsRepo.getSnapshot()
            val cloud = CloudBackupRepository.pullGeneralSettings()
            when (decideSync(local.updatedAt, cloud?.updatedAt)) {
                SyncDecision.APPLY_REMOTE -> settingsRepo.applyRemote(checkNotNull(cloud).toLocal())
                SyncDecision.PUSH_LOCAL -> CloudBackupRepository.pushGeneralSettings(local.toCloud())
            }
        } catch (_: Exception) {
            // Local General Settings left untouched.
        }

        try {
            val profileRepo = UserProfileRepository(application)
            val local = profileRepo.getProfile(defaultName = AuthRepository.currentUser?.displayName.orEmpty())
            val localUpdatedAt = profileRepo.getUpdatedAt()
            val cloudProfile = CloudBackupRepository.pullProfile()
            when (decideSync(localUpdatedAt, cloudProfile?.updatedAt)) {
                SyncDecision.APPLY_REMOTE -> {
                    checkNotNull(cloudProfile)
                    profileRepo.applyRemote(
                        name = cloudProfile.name,
                        dateOfBirth = cloudProfile.dateOfBirth,
                        gender = cloudProfile.gender?.let { raw -> runCatching { Gender.valueOf(raw) }.getOrNull() },
                        updatedAt = cloudProfile.updatedAt
                    )
                }
                SyncDecision.PUSH_LOCAL -> CloudBackupRepository.pushProfile(
                    CloudProfile(local.name, local.dateOfBirth, local.gender?.name, localUpdatedAt)
                )
            }

            // The photo is a file, not a preference value, so it's restored separately: only
            // when this install doesn't already have one locally (a fresh install/reinstall is
            // exactly when that's true) - never overwrites an already-present local photo, and
            // never deletes the cloud copy just because a download attempt found nothing to
            // restore from (see CloudBackupRepository.downloadProfilePhoto's own doc comment).
            val photoFile = ProfilePhotoStorage.photoFile(application)
            if (!photoFile.exists()) {
                CloudBackupRepository.downloadProfilePhoto(photoFile)
            }
        } catch (_: Exception) {
            // Local Profile left untouched.
        }
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
                    // Only when this lands directly on Home: NavGraph routes a still-blank
                    // displayName to Set Up Profile instead, whose own submitProfileSetup()
                    // already runs this same sync once profile setup finishes - calling it here
                    // too for that branch would just be a redundant duplicate sync for an
                    // account that, being brand new, has nothing to restore yet anyway.
                    if (!AuthRepository.currentUser?.displayName.isNullOrBlank()) {
                        syncAfterSignIn()
                    }
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

/** Local<->cloud conversion for General Settings - kept as plain, internal top-level functions
 * (not part of GeneralSettingsRepository/CloudBackupRepository themselves) since this specific
 * enum<->String mapping, with its specific unrecognized-value fallbacks, is only needed at sync
 * boundaries: [syncAfterSignIn] (restore) and [DataPrivacyViewModel.backupNow] (push-on-demand). */
internal fun GeneralSettingsSnapshot.toCloud(): CloudGeneralSettings = CloudGeneralSettings(
    startScreen = startScreen.name,
    defaultTimerMinutes = defaultTimerMinutes,
    defaultTag = defaultTag,
    defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
    focusSessionNotificationsEnabled = focusSessionNotificationsEnabled,
    focusSessionCompleteEnabled = focusSessionCompleteEnabled,
    weekStartDay = weekStartDay.name,
    timeFormat = timeFormat.name,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    notesViewMode = notesViewMode.name,
    updatedAt = updatedAt
)

internal fun CloudGeneralSettings.toLocal(): GeneralSettingsSnapshot = GeneralSettingsSnapshot(
    startScreen = runCatching { StartScreen.valueOf(startScreen) }.getOrDefault(StartScreen.TASKS),
    defaultTimerMinutes = defaultTimerMinutes,
    defaultTag = defaultTag,
    defaultPostponeIfIncomplete = defaultPostponeIfIncomplete,
    focusSessionNotificationsEnabled = focusSessionNotificationsEnabled,
    focusSessionCompleteEnabled = focusSessionCompleteEnabled,
    weekStartDay = runCatching { DayOfWeek.valueOf(weekStartDay) }.getOrDefault(DayOfWeek.MONDAY),
    timeFormat = runCatching { TimeFormat.valueOf(timeFormat) }.getOrDefault(TimeFormat.SYSTEM_DEFAULT),
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    notesViewMode = runCatching { NotesViewMode.valueOf(notesViewMode) }.getOrDefault(NotesViewMode.LIST),
    updatedAt = updatedAt
)
