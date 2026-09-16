package com.nj031.onetask.data.profile

import android.content.Context

enum class Gender { MALE, FEMALE, PREFER_NOT_TO_SAY }

data class UserProfile(
    val name: String,
    val dateOfBirth: Long? = null,
    val gender: Gender? = null,
    val photoPath: String? = null
)

private const val PREFS_NAME = "user_profile_prefs"
private const val KEY_HAS_PROFILE = "has_profile"
private const val KEY_NAME = "name"
private const val KEY_DOB = "date_of_birth"
private const val KEY_GENDER = "gender"
private const val KEY_PHOTO_PATH = "photo_path"

/**
 * Stores the user's editable profile fields (name/date of birth/gender/photo path) in a private
 * SharedPreferences file rather than a new Room table, deliberately: AppDatabase has no real
 * Migration objects and falls back to `fallbackToDestructiveMigration()` on any version bump, so
 * adding a new @Entity here would silently wipe every existing installs's Tasks/Journal data on
 * their next update - unacceptable just to add a handful of simple profile fields. SharedPreferences
 * needs no schema/migration at all and already persists across app restarts and logout/login on
 * the same device (Firebase signOut() never touches local storage), which is everything this
 * feature's persistence requirements call for.
 */
class UserProfileRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** [defaultName] (the signed-in Google account's display name) is only used the very first
     * time this is called for a device that has never saved a profile before; once the user
     * saves via Edit Profile, the saved name always takes over. */
    fun getProfile(defaultName: String): UserProfile {
        if (!prefs.contains(KEY_HAS_PROFILE)) {
            return UserProfile(name = defaultName)
        }
        return UserProfile(
            name = prefs.getString(KEY_NAME, defaultName) ?: defaultName,
            dateOfBirth = if (prefs.contains(KEY_DOB)) prefs.getLong(KEY_DOB, 0L) else null,
            gender = prefs.getString(KEY_GENDER, null)?.let { raw -> runCatching { Gender.valueOf(raw) }.getOrNull() },
            photoPath = prefs.getString(KEY_PHOTO_PATH, null)
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putBoolean(KEY_HAS_PROFILE, true)
            putString(KEY_NAME, profile.name)
            if (profile.dateOfBirth != null) putLong(KEY_DOB, profile.dateOfBirth) else remove(KEY_DOB)
            if (profile.gender != null) putString(KEY_GENDER, profile.gender.name) else remove(KEY_GENDER)
            if (profile.photoPath != null) putString(KEY_PHOTO_PATH, profile.photoPath) else remove(KEY_PHOTO_PATH)
        }.apply()
    }
}
