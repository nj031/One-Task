package com.nj031.onetask.data.profile

import android.content.Context
import com.nj031.onetask.data.UserScopedPreferences

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
private const val KEY_UPDATED_AT = "updated_at"

/**
 * Stores the user's editable profile fields (name/date of birth/gender/photo path) in a private
 * SharedPreferences file rather than a new Room table, deliberately: AppDatabase has no real
 * Migration objects and falls back to `fallbackToDestructiveMigration()` on any version bump, so
 * adding a new @Entity here would silently wipe every existing installs's Tasks/Journal data on
 * their next update - unacceptable just to add a handful of simple profile fields. SharedPreferences
 * needs no schema/migration at all and already persists across app restarts, which is everything
 * this feature's persistence requirements call for. The file itself is scoped per signed-in
 * account (see [UserScopedPreferences]) - a plain fixed file name would mean every account on
 * this device shared the exact same saved name/DOB/gender/photo.
 */
class UserProfileRepository(context: Context) {
    private val prefs = UserScopedPreferences.open(context, PREFS_NAME)

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
            putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.apply()
    }

    fun getUpdatedAt(): Long = prefs.getLong(KEY_UPDATED_AT, 0L)

    /** Applies a cloud-restored Profile (name/DOB/gender only - see
     * [com.nj031.onetask.data.sync.CloudBackupRepository]'s own doc comment for why the photo is
     * backed by Storage instead of this Firestore document) locally. Used only by the
     * post-sign-in restore, never by [saveProfile] (Edit Profile's own save action) - writes
     * [updatedAt] as given (the cloud value being restored), not "now", so this restore is never
     * mistaken for a newer local edit on the very next sync. The local photoPath is left exactly
     * as it already is; photo restoration is a separate step (see ProfilePhotoStorage /
     * CloudBackupRepository.downloadProfilePhoto), since it's a file, not a preference value. */
    fun applyRemote(name: String, dateOfBirth: Long?, gender: Gender?, updatedAt: Long) {
        prefs.edit().apply {
            putBoolean(KEY_HAS_PROFILE, true)
            putString(KEY_NAME, name)
            if (dateOfBirth != null) putLong(KEY_DOB, dateOfBirth) else remove(KEY_DOB)
            if (gender != null) putString(KEY_GENDER, gender.name) else remove(KEY_GENDER)
            putLong(KEY_UPDATED_AT, updatedAt)
        }.apply()
    }
}
