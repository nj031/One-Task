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
import com.nj031.onetask.R
import kotlinx.coroutines.tasks.await

/** Thin wrapper around Firebase Auth's Google sign-in flow. */
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

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: error("Sign-in succeeded but returned no user")
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
