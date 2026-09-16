package com.nj031.onetask.data.feedback

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nj031.onetask.data.auth.AuthRepository
import kotlinx.coroutines.tasks.await

enum class FeedbackType(val backendValue: String) {
    FEATURE("feature"),
    BUG("bug"),
    FEEDBACK("feedback");

    companion object {
        fun fromRouteValue(value: String): FeedbackType =
            entries.firstOrNull { it.backendValue == value } ?: FEEDBACK
    }
}

/**
 * Submits Help & Feedback entries (feature suggestions, bug reports, general feedback) to a
 * single Firestore "feedback_submissions" collection, distinguished by a [FeedbackType] field -
 * separate from CloudBackupRepository's per-user "users/{uid}/..." mirroring, since these are
 * one-way submissions to the app's maintainers rather than the user's own synced data. Unlike
 * CloudBackupRepository's fire-and-forget writes, submissions here always await Firestore's
 * confirmation so the UI can show a real success/failure state rather than an optimistic one.
 */
object FeedbackRepository {
    private const val COLLECTION = "feedback_submissions"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun submit(
        type: FeedbackType,
        message: String,
        appVersion: String,
        androidVersion: String? = null,
        deviceModel: String? = null
    ) {
        val uid = AuthRepository.currentUser?.uid ?: error("You need to be signed in to do this.")
        val data = mutableMapOf<String, Any>(
            "type" to type.backendValue,
            "userId" to uid,
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion
        )
        if (androidVersion != null) data["androidVersion"] = androidVersion
        if (deviceModel != null) data["deviceModel"] = deviceModel
        firestore.collection(COLLECTION).add(data).await()
    }
}
