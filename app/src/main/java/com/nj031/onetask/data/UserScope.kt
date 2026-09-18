package com.nj031.onetask.data

import com.nj031.onetask.data.auth.AuthRepository

/**
 * The storage-scoping identifier for whichever account is currently signed in. Every per-user
 * store in this app - the Room database file, every SharedPreferences file, and the profile
 * photo file - is namespaced by this, so switching accounts on the same device (or the same
 * process staying alive across a sign-out/sign-in) can never read or write another account's
 * data.
 *
 * Before this existed, every one of those stores used a single fixed, device-wide file name, so
 * every account signed into on a device shared exactly the same Tasks/Notes/Profile/Settings -
 * this is the fix for that.
 */
object UserScope {
    /** Used only for the (should be unreachable in practice - every data-bearing screen requires
     * sign-in, per NavGraph's own startDestination logic) case of a storage call happening with
     * nobody signed in, so it can never crash or fall back to a shared/ambiguous file name. */
    private const val NO_ACCOUNT_ID = "no_account"

    /** Firebase UID of the signed-in account, or [NO_ACCOUNT_ID] if nobody is signed in. */
    fun id(): String = AuthRepository.currentUser?.uid ?: NO_ACCOUNT_ID

    /** Appends the current account's scoping id to a base storage name (a SharedPreferences file
     * name or a Room database name), e.g. "user_profile_prefs" -> "user_profile_prefs_a1b2c3". */
    fun scoped(baseName: String): String = "${baseName}_${id()}"
}
