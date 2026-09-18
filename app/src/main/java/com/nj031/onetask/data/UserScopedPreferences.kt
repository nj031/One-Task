package com.nj031.onetask.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Opens a per-account SharedPreferences file (see [UserScope]) instead of the single, unscoped
 * file every account on this device used to share before per-account storage existed.
 *
 * The very first account to open a given [baseName] store after this existed inherits whatever
 * was already saved in the old shared file - nothing is deleted, the legacy file is simply left
 * in place (untouched, no longer read from) once this runs once. Every OTHER account that opens
 * the same store afterward starts from empty/default values instead of also inheriting it, since
 * there is no way to know which of the old file's values were really theirs - that was never
 * recorded - and guessing risks a worse privacy bug than empty defaults for those accounts. This
 * mirrors [AppDatabase]'s own one-time, non-destructive claim of the legacy Room database.
 */
object UserScopedPreferences {
    private const val CLAIM_FLAGS_PREFS_NAME = "legacy_prefs_claim_flags"

    fun open(context: Context, baseName: String): SharedPreferences {
        val appContext = context.applicationContext
        val scopedName = UserScope.scoped(baseName)
        claimLegacyIfNeeded(appContext, baseName, scopedName)
        return appContext.getSharedPreferences(scopedName, Context.MODE_PRIVATE)
    }

    private fun claimLegacyIfNeeded(context: Context, legacyName: String, scopedName: String) {
        val claimFlags = context.getSharedPreferences(CLAIM_FLAGS_PREFS_NAME, Context.MODE_PRIVATE)
        val claimKey = "claimed_$legacyName"
        if (claimFlags.getBoolean(claimKey, false)) return
        val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        val legacyEntries = legacy.all
        if (legacyEntries.isNotEmpty()) {
            val scoped = context.getSharedPreferences(scopedName, Context.MODE_PRIVATE)
            val editor = scoped.edit()
            for ((key, value) in legacyEntries) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                    else -> Unit
                }
            }
            editor.apply()
        }
        claimFlags.edit().putBoolean(claimKey, true).apply()
    }
}
