package com.nj031.onetask.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.nj031.onetask.R
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around Firebase Auth - Google Sign-In, email/password, and account management.
 * All of these operate on the same underlying FirebaseAuth session, so switching between
 * sign-in methods (or signing out and back in with the same account) never creates a second,
 * separate account.
 */
object AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser? get() = auth.currentUser

    /**
     * Requests a fresh Google ID token via the system account picker. Shared by the initial
     * sign-in flow (AuthViewModel) and by account deletion's re-authentication path: Firebase
     * requires a *recent* sign-in before it will permanently delete an account, so a
     * long-signed-in session may need to re-prove identity here before delete() will succeed.
     */
    suspend fun requestGoogleIdToken(context: Context): String {
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

    /** [isNewUser] lets callers tell a first-time Google account creation apart from an
     * existing account signing back in - used to decide whether Set Up Profile is needed,
     * without ever guessing from [FirebaseUser.displayName] alone (which can legitimately be
     * blank on a long-existing account that already has a profile name saved locally). */
    data class GoogleSignInResult(val user: FirebaseUser, val isNewUser: Boolean)

    suspend fun signInWithGoogleIdToken(idToken: String): GoogleSignInResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Sign-in succeeded but returned no user")
        return GoogleSignInResult(user, result.additionalUserInfo?.isNewUser == true)
    }

    /** Creates a new email/password account. Firebase signs the new user in immediately - no
     * separate sign-in call is needed afterward. */
    suspend fun createAccountWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user ?: error("Account creation succeeded but returned no user")
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: error("Sign-in succeeded but returned no user")
    }

    /** Updates the password on the currently signed-in account - used when a user backs up
     * during account creation (after the account already exists but before it's verified) and
     * re-submits a different password, so a retry updates the pending account instead of
     * failing with "email already in use". */
    suspend fun updateCurrentPassword(newPassword: String) {
        auth.currentUser?.updatePassword(newPassword)?.await()
    }

    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    /** Refreshes the cached FirebaseUser from the server - required before trusting
     * [isCurrentUserEmailVerified], since verifying via the emailed link happens outside the
     * app and the local FirebaseUser object doesn't update on its own. */
    suspend fun reloadCurrentUser() {
        auth.currentUser?.reload()?.await()
    }

    val isCurrentUserEmailVerified: Boolean get() = auth.currentUser?.isEmailVerified ?: false

    suspend fun updateDisplayName(name: String) {
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        auth.currentUser?.updateProfile(request)?.await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth.signOut()
    }

    /** Permanently deletes the signed-in Firebase Auth account. Callers should catch
     * [com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException] and retry after a
     * fresh [requestGoogleIdToken] + [signInWithGoogleIdToken] re-authentication. */
    suspend fun deleteAccount() {
        auth.currentUser?.delete()?.await()
    }
}
