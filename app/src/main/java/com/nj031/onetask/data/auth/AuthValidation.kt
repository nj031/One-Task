package com.nj031.onetask.data.auth

import android.util.Patterns

object AuthValidation {
    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** At least 8 characters, at least one letter, at least one number - matches the
     * requirement text shown on the Set Up Password screen. */
    fun isValidPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}
