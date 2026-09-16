package com.nj031.onetask.data.profile

import android.content.Context
import android.net.Uri
import java.io.File

private const val PROFILE_PHOTO_FILENAME = "profile_photo.jpg"

/** Copies a picked profile photo into the app's private storage under a single fixed filename,
 * so changing the photo overwrites the previous one rather than accumulating orphaned files. */
object ProfilePhotoStorage {
    fun photoFile(context: Context): File = File(context.filesDir, PROFILE_PHOTO_FILENAME)

    fun saveFrom(context: Context, source: Uri): String {
        val destination = photoFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination.absolutePath
    }

    fun delete(context: Context) {
        val file = photoFile(context)
        if (file.exists()) file.delete()
    }
}
