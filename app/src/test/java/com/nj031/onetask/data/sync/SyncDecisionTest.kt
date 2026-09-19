package com.nj031.onetask.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain JUnit tests for [decideSync] - the merge rule [com.nj031.onetask.viewmodel.AuthViewModel
 * .syncAfterSignIn] uses to decide, for each of Appearance/General Settings/Profile, whether a
 * post-sign-in restore should apply the cloud copy locally or push the local copy up. Safe to
 * run without Robolectric/Android since the function under test has zero Android/Firebase
 * dependencies. */
class SyncDecisionTest {
    @Test
    fun `no cloud document yet pushes local, even when local is just untouched defaults`() {
        // The critical "fresh install/first sync must never look like the user wants their
        // data deleted" case: local updatedAt 0 (never touched) with no cloud document at all
        // must still push local (seeding the cloud), never silently do nothing.
        assertEquals(SyncDecision.PUSH_LOCAL, decideSync(localUpdatedAt = 0L, cloudUpdatedAt = null))
    }

    @Test
    fun `no cloud document yet pushes already-customized local data`() {
        // An existing user who customized a setting before this account-sync feature ever
        // existed: their local data must be preserved and uploaded, not discarded.
        assertEquals(SyncDecision.PUSH_LOCAL, decideSync(localUpdatedAt = 5_000L, cloudUpdatedAt = null))
    }

    @Test
    fun `empty untouched local never overwrites existing cloud data`() {
        // The exact "fresh install after reinstall" scenario: local is naturally at its
        // just-constructed default (updatedAt 0) while the cloud already has real, previously
        // synced data - the cloud copy must win.
        assertEquals(SyncDecision.APPLY_REMOTE, decideSync(localUpdatedAt = 0L, cloudUpdatedAt = 1_000L))
    }

    @Test
    fun `newer cloud copy wins over older local copy`() {
        assertEquals(SyncDecision.APPLY_REMOTE, decideSync(localUpdatedAt = 1_000L, cloudUpdatedAt = 2_000L))
    }

    @Test
    fun `newer local copy wins and is pushed over an older cloud copy`() {
        // e.g. a setting changed while offline, before this device's copy had a chance to sync.
        assertEquals(SyncDecision.PUSH_LOCAL, decideSync(localUpdatedAt = 2_000L, cloudUpdatedAt = 1_000L))
    }

    @Test
    fun `exact tie goes to the cloud copy, deterministically`() {
        assertEquals(SyncDecision.APPLY_REMOTE, decideSync(localUpdatedAt = 1_000L, cloudUpdatedAt = 1_000L))
    }
}
