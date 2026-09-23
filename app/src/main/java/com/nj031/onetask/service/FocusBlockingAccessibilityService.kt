package com.nj031.onetask.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 7 Part 1 foundation only: detects foreground-app (window) changes via the standard
 * TYPE_WINDOW_STATE_CHANGED accessibility event and extracts the foreground package name -
 * nothing more. Deliberately takes no blocking action yet (no performGlobalAction, no redirect,
 * no reading of Focus state to decide anything) - that enforcement logic is Phase 7 Part 2's job.
 * See res/xml/focus_blocking_accessibility_service_config.xml for this service's declared
 * capabilities (window-state events only, no window-content retrieval - the minimum needed for
 * package-name detection).
 *
 * This is a separate Android framework component from TimerViewModel/FocusOverlayState: it is
 * enabled independently by the user in system Accessibility settings (see
 * FocusAccessibilityUtil.isFocusBlockingServiceEnabled) and, once enabled, runs regardless of
 * whether the app's Activity/ViewModel is alive - which is exactly why the Focus-blocking state a
 * later part will read (TimerSessionRepository.focusBlockingSnapshot) is mirrored into the same
 * durable, per-account SharedPreferences store the rest of the Timer/Stopwatch feature already
 * uses, rather than left only in the UI-scoped FocusOverlayState (see that class's own doc
 * comment for why it alone is not sufficient for this).
 */
class FocusBlockingAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        // Part 2 will read TimerSessionRepository's focus-blocking mirror here and, if
        // foregroundPackage is listed and no break is active, intervene. Intentionally inert for
        // now - this log line only proves detection itself is wired correctly.
        Log.d(TAG, "Foreground package changed: $foregroundPackage")
    }

    override fun onInterrupt() {
        // No ongoing feedback/action for this service to interrupt yet (Part 1 takes no action).
    }

    private companion object {
        private const val TAG = "FocusBlockingA11yService"
    }
}
