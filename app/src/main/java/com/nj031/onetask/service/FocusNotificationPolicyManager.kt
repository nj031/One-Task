package com.nj031.onetask.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.nj031.onetask.data.timer.FocusCallsMode
import com.nj031.onetask.data.timer.FocusNotificationPolicySnapshot
import com.nj031.onetask.data.timer.FocusNotificationsMode

/**
 * Phase 12: applies/restores the system-level DND (NotificationManager interruption filter) and
 * ringer-mode (AudioManager) state a Focus session's Notifications/Calls settings require.
 *
 * Deliberately two independent Android mechanisms, not one: DND's Policy/suppressedVisualEffects
 * system expresses "hide notifications" and "mute notifications" (Notifications' 3 modes), while
 * plain ringerMode expresses "let the call ring through but silently" (Calls' 2 modes, which
 * never blocks/rejects a call - only NotificationManager's own priority-category bitmask could
 * fold calls into the same system as notifications, but that would make "calls silent, everything
 * else normal" version-sensitive and needlessly complex for what this feature actually needs).
 *
 * Both require the same "Notification Policy Access" special app permission on Android 6+ (no
 * AndroidManifest.xml declaration needed - unlike FocusBlockingAccessibilityService's Accessibility
 * permission, this one has no <service>/<uses-permission> entry at all, only a runtime check and a
 * deep link to system Settings). Every apply/restore call below is guarded by
 * [isNotificationPolicyAccessGranted] and silently no-ops without it, mirroring
 * FocusAccessibilityUtil's "service disabled mid-session: do not crash, simply do not act" pattern
 * - setInterruptionFilter/ringerMode both throw SecurityException without this permission.
 */
object FocusNotificationPolicyManager {

    /** Whether the user has granted One Task Notification Policy Access (DND access) in system
     * Settings - Android provides no in-app runtime permission dialog for this, only this check
     * and a deep link (Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) - see
     * FocusModeConfigScreen's "Save & Start Focus" gate, which re-checks this fresh on every tap
     * rather than caching it, exactly like FocusAccessibilityUtil's own equivalent check. */
    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /** Captures the system's current DND/ringer state, cheap to call unconditionally at Focus
     * session start regardless of which modes are selected - see [restore] for why this, rather
     * than a hardcoded revert target, is what every restore call uses. */
    fun captureCurrentState(context: Context): FocusNotificationPolicySnapshot {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return FocusNotificationPolicySnapshot(
            interruptionFilter = notificationManager.currentInterruptionFilter,
            ringerMode = audioManager.ringerMode
        )
    }

    /** Applies the given Notifications/Calls modes now - each independently a no-op when its mode
     * is ALLOW, so e.g. Notifications=NONE + Calls=Allow only ever touches the interruption
     * filter, never the ringer mode. No-ops entirely (both dimensions) if Notification Policy
     * Access isn't currently granted. */
    fun applyPolicy(context: Context, notificationsMode: FocusNotificationsMode, callsMode: FocusCallsMode) {
        if (!isNotificationPolicyAccessGranted(context)) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (notificationsMode) {
            FocusNotificationsMode.ALLOW -> {}
            FocusNotificationsMode.SILENT -> {
                notificationManager.setNotificationPolicy(
                    NotificationManager.Policy(
                        0,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                        NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS
                    )
                )
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
            FocusNotificationsMode.NONE -> {
                notificationManager.setNotificationPolicy(
                    NotificationManager.Policy(
                        0,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                        NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE or
                            NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST
                    )
                )
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        }

        when (callsMode) {
            FocusCallsMode.ALLOW -> {}
            FocusCallsMode.SILENT -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        }
    }

    /** Restores the system's DND/ringer state to exactly what [captureCurrentState] captured -
     * never a hardcoded "ALL"/"NORMAL" - so a user's own manually-set DND/ringer configuration is
     * never clobbered by a Focus session ending. No-ops entirely if Notification Policy Access
     * isn't currently granted (matching [applyPolicy] - if the permission was revoked mid-session,
     * there is nothing for this app to restore either). */
    fun restore(context: Context, snapshot: FocusNotificationPolicySnapshot) {
        if (!isNotificationPolicyAccessGranted(context)) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager.setInterruptionFilter(snapshot.interruptionFilter)
        audioManager.ringerMode = snapshot.ringerMode
    }
}
