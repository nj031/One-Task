package com.nj031.onetask.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Opens a per-account SharedPreferences file (see [UserScope]) instead of the single, unscoped
 * file every account on this device used to share before per-account storage existed.
 *
 * Deliberately does NOT claim/copy anything from that old shared file: there is no reliable way
 * to know which account the values in it actually belonged to (that was never recorded), and
 * assigning it to whichever account happens to open a store first - even the very first one -
 * is an unverified guess, not a real ownership determination. The legacy file is simply left on
 * disk, untouched and unread, forever; every account (including the first one ever to sign in
 * after per-account storage was introduced) starts from genuinely empty defaults instead.
 */
object UserScopedPreferences {
    fun open(context: Context, baseName: String): SharedPreferences {
        val appContext = context.applicationContext
        val scopedName = UserScope.scoped(baseName)
        return appContext.getSharedPreferences(scopedName, Context.MODE_PRIVATE)
    }
}
