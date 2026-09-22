package com.nj031.onetask.data.journal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Local, per-note storage for a note's (at most one, for now) image block, mirroring
 * [com.nj031.onetask.data.profile.ProfilePhotoStorage]'s own local-file convention exactly. A
 * fixed filename derived from the note's own id - rather than the account, like the profile
 * photo - means adding a new image always overwrites any previous one for THAT note rather than
 * accumulating orphaned files, which is exactly correct given the current one-image-per-note
 * limit (see [NoteBlockType.IMAGE]'s own doc comment).
 *
 * Deliberately NOT account-scoped the way [com.nj031.onetask.data.profile.ProfilePhotoStorage]
 * is: a note's own id is already globally unique (a random UUID - see [JournalNoteEntity]'s own
 * default), so two different accounts' notes can never collide on the same file even on a shared
 * device, without needing the id itself to also carry the signed-in account's uid.
 */
object NoteImageStorage {
    private fun imagesDir(context: Context): File =
        File(context.filesDir, "note_images").apply { mkdirs() }

    fun fileFor(context: Context, noteId: String): File = File(imagesDir(context), "$noteId.jpg")

    /** Copies a picked image into this note's own local file. Returns null (leaving nothing
     * written) if the source can't actually be read, rather than leaving a partially-written or
     * stale file behind. */
    fun saveFrom(context: Context, noteId: String, source: Uri): String? {
        val destination = fileFor(context, noteId)
        return try {
            context.contentResolver.openInputStream(source)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            destination.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** A content:// [Uri] the system Camera app is allowed to write the captured photo directly
     * to - already this note's own final local file location (see [fileFor]), so a successful
     * capture needs no further copy step; the returned Uri's underlying file is exactly what
     * [fileFor] already points at. The "${applicationId}.fileprovider" authority is declared
     * against this same app-private files directory in AndroidManifest.xml/res/xml/file_paths.xml
     * - the standard Android mechanism for handing another app (the Camera app) permission to
     * write into a directory this app doesn't otherwise expose. */
    fun createCaptureUri(context: Context, noteId: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileFor(context, noteId))

    fun delete(context: Context, noteId: String) {
        val file = fileFor(context, noteId)
        if (file.exists()) file.delete()
    }
}
