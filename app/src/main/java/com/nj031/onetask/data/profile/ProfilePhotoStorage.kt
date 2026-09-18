package com.nj031.onetask.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.nj031.onetask.data.UserScope
import java.io.File

private const val LEGACY_PROFILE_PHOTO_FILENAME = "profile_photo.jpg"
private const val CLAIM_FLAGS_PREFS_NAME = "legacy_photo_claim_flags"
private const val KEY_LEGACY_PHOTO_CLAIMED = "legacy_photo_claimed"

/** Copies a picked profile photo into the app's private storage under a filename scoped to the
 * signed-in account (see [UserScope]) - a single fixed filename would mean every account on this
 * device shared the exact same photo file, so changing it as one account would silently change
 * what every other account's Profile screen shows too. Changing the photo overwrites the
 * previous one for THAT account rather than accumulating orphaned files. */
object ProfilePhotoStorage {
    fun photoFile(context: Context): File {
        claimLegacyPhotoIfNeeded(context)
        return File(context.filesDir, "profile_photo_${UserScope.id()}.jpg")
    }

    fun saveFrom(context: Context, source: Uri): String {
        val destination = photoFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination.absolutePath
    }

    /** Used by the profile photo crop flow, which produces an in-memory cropped Bitmap rather
     * than a source Uri to copy bytes from. */
    fun saveBitmap(context: Context, bitmap: Bitmap): String {
        val destination = photoFile(context)
        destination.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output) }
        return destination.absolutePath
    }

    fun delete(context: Context) {
        val file = photoFile(context)
        if (file.exists()) file.delete()
    }

    /** One-time, non-destructive claim: the very first account to read/write its photo file
     * after per-account photos were introduced inherits whatever the single photo every account
     * previously shared was - nothing is deleted, the legacy file is simply left in place
     * (untouched, no longer read from) once this runs. Every OTHER account starts with no photo
     * instead of also inheriting it, since there is no way to know whose photo it really was.
     * Mirrors AppDatabase's own claimLegacyDatabaseIfNeeded. */
    private fun claimLegacyPhotoIfNeeded(context: Context) {
        val flags = context.getSharedPreferences(CLAIM_FLAGS_PREFS_NAME, Context.MODE_PRIVATE)
        if (flags.getBoolean(KEY_LEGACY_PHOTO_CLAIMED, false)) return
        val legacyFile = File(context.filesDir, LEGACY_PROFILE_PHOTO_FILENAME)
        if (legacyFile.exists()) {
            val newFile = File(context.filesDir, "profile_photo_${UserScope.id()}.jpg")
            if (!newFile.exists()) {
                runCatching { legacyFile.copyTo(newFile, overwrite = false) }
            }
        }
        flags.edit().putBoolean(KEY_LEGACY_PHOTO_CLAIMED, true).apply()
    }
}
