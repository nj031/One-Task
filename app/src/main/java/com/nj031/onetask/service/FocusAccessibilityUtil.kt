package com.nj031.onetask.service

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/**
 * Whether the user has manually enabled [FocusBlockingAccessibilityService] in system
 * Accessibility settings. Android provides no in-app runtime permission dialog for this and no
 * way for the app to enable it itself or be notified when it changes - only to check the current
 * state on demand (see FocusModeConfigScreen's "Save & Start Focus" gate, which re-checks this
 * fresh on every tap rather than caching it).
 */
object FocusAccessibilityUtil {
    /** Standard technique for checking whether a specific accessibility service is currently
     * enabled: Android exposes no dedicated API for this, only the colon-separated list of every
     * enabled service's flattened component name in this Settings.Secure value. */
    fun isFocusBlockingServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${FocusBlockingAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
