package com.nj031.onetask.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain JUnit tests for the pure Firestore/Storage path builders [CloudBackupRepository]'s
 * every read/write resolves through - the actual account-isolation boundary. Safe to run without
 * Robolectric/Android since these are plain string functions with zero Firebase dependency; see
 * that file's own comment for why the SDK accepts these exact path strings. */
class CloudPathsTest {
    private val uidA = "uid-account-a"
    private val uidB = "uid-account-b"

    @Test
    fun `every path is namespaced under users slash the given uid`() {
        assertEquals("users/$uidA/tasks", tasksPath(uidA))
        assertEquals("users/$uidA/notes", notesPath(uidA))
        assertEquals("users/$uidA/tags", tagsPath(uidA))
        assertEquals("users/$uidA/categories", categoriesPath(uidA))
        assertEquals("users/$uidA/labels", labelsPath(uidA))
        assertEquals("users/$uidA/account", accountPath(uidA))
    }

    @Test
    fun `two different accounts never resolve to the same path for any collection`() {
        assertNotEquals(tasksPath(uidA), tasksPath(uidB))
        assertNotEquals(notesPath(uidA), notesPath(uidB))
        assertNotEquals(tagsPath(uidA), tagsPath(uidB))
        assertNotEquals(categoriesPath(uidA), categoriesPath(uidB))
        assertNotEquals(labelsPath(uidA), labelsPath(uidB))
        assertNotEquals(accountPath(uidA), accountPath(uidB))
        assertNotEquals(profilePhotoPath(uidA), profilePhotoPath(uidB))
    }

    @Test
    fun `neither account's path is a prefix or suffix of the other's for the same collection`() {
        // Guards against a narrower uid accidentally being a Firestore path-segment prefix of a
        // longer one (which would not actually cause a collision in Firestore's data model, but
        // this keeps the invariant explicit and simple to reason about).
        assertTrue(!tasksPath(uidA).contains(uidB) && !tasksPath(uidB).contains(uidA))
    }

    @Test
    fun `different collection types for the same account never collide with each other`() {
        val allPaths = listOf(
            tasksPath(uidA), notesPath(uidA), tagsPath(uidA), categoriesPath(uidA), labelsPath(uidA), accountPath(uidA)
        )
        assertEquals(allPaths.size, allPaths.toSet().size)
    }

    @Test
    fun `account sub-document paths are namespaced per account and per key`() {
        assertTrue(accountPath(uidA).endsWith("/account"))
        assertTrue(accountPath(uidA).contains(uidA))
        assertNotEquals(accountPath(uidA), accountPath(uidB))
    }

    @Test
    fun `profile photo path is uid-scoped, never a shared or global path`() {
        assertEquals("profile_photos/$uidA.jpg", profilePhotoPath(uidA))
        assertTrue(profilePhotoPath(uidA).contains(uidA))
        assertNotEquals(profilePhotoPath(uidA), profilePhotoPath(uidB))
    }
}
