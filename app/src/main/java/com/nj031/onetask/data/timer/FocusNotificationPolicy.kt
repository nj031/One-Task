package com.nj031.onetask.data.timer

/** Notifications behaviour during an active Focus session - see FocusNotificationPolicyManager
 * for how each mode is actually applied via NotificationManager. */
enum class FocusNotificationsMode { NONE, SILENT, ALLOW }

/** Calls behaviour during an active Focus session - deliberately only 2 states (no "block calls"
 * option, per spec) - see FocusNotificationPolicyManager for how each mode is actually applied
 * via AudioManager.ringerMode. */
enum class FocusCallsMode { SILENT, ALLOW }

/** The system's own DND/ringer state captured once at Focus session start (before either mode is
 * applied) so it can be restored exactly - never reset to a hardcoded "ALL"/"NORMAL" - once
 * restrictions end (break start, Stop, or natural completion). See FocusNotificationPolicyManager
 * for where this is captured and restored. */
data class FocusNotificationPolicySnapshot(
    val interruptionFilter: Int,
    val ringerMode: Int
)
