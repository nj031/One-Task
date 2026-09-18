package com.nj031.onetask.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.nj031.onetask.data.UserScope
import java.io.File

/** Copies a picked profile photo into the app's private storage under a filename scoped to the
 * signed-in account (see [UserScope]) - a single fixed filename would mean every account on this
 * device shared the exact same photo file, so changing it as one account would silently change
 * what every other account's Profile screen shows too. Changing the photo overwrites the
 * previous one for THAT account rather than accumulating orphaned files.
 *
 * Deliberately does NOT claim/copy the legacy, pre-per-account photo file every install prior to
 * per-account storage used: there is no reliable way to know which account it actually belonged
 * to, and assigning it to whichever account happens to read/write a photo first is an unverified
 * guess, not a real ownership determination. That file is simply left on disk, untouched and
 * unread, forever; every account (including the very first to sign in after per-account storage
 * was introduced) starts with no photo instead. */
object ProfilePhotoStorage {
    fun photoFile(context: Context): File =
        File(context.filesDir, "profile_photo_${UserScope.id()}.jpg")

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
}
