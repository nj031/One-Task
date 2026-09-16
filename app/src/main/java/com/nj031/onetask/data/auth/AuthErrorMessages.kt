package com.nj031.onetask.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

enum class AuthErrorContext { SIGN_UP, LOGIN, RESET_PASSWORD }

/** Maps Firebase Auth's exception types to short, non-technical messages - callers should
 * never show a raw exception message or stack trace to the user. */
object AuthErrorMessages {
    fun from(e: Exception, context: AuthErrorContext): String = when (e) {
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 8 characters with a letter and a number."
        is FirebaseAuthUserCollisionException ->
            "An account with this email already exists."
        is FirebaseAuthInvalidUserException ->
            if (context == AuthErrorContext.LOGIN) "Incorrect email or password." else "No account found with this email."
        is FirebaseAuthInvalidCredentialsException ->
            if (context == AuthErrorContext.LOGIN) "Incorrect email or password." else "Please enter a valid email address."
        is FirebaseTooManyRequestsException ->
            "Too many attempts. Please wait a moment and try again."
        is FirebaseNetworkException ->
            "Network error. Please check your connection and try again."
        else ->
            "Something went wrong. Please try again."
    }
}
