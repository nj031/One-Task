package com.nj031.onetask.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nj031.onetask.data.timer.TimerSessionRepository

/**
 * Phase 7 Part 2: enforces Focus app blocking. Detects foreground-app (window) changes via the
 * standard TYPE_WINDOW_STATE_CHANGED accessibility event and, if the app that just came to
 * foreground is one of the currently blocked packages and no Focus break is active, sends the
 * user Home via performGlobalAction(GLOBAL_ACTION_HOME) - the minimal Phase 7 blocking action.
 * See res/xml/focus_blocking_accessibility_service_config.xml for this service's declared
 * capabilities (window-state events only, no window-content retrieval - the minimum needed for
 * package-name detection; no capability beyond that is used here either).
 *
 * This is a separate Android framework component from TimerViewModel/FocusOverlayState: it is
 * enabled independently by the user in system Accessibility settings (see
 * FocusAccessibilityUtil.isFocusBlockingServiceEnabled) and, once enabled, runs regardless of
 * whether the app's Activity/ViewModel is alive. That is exactly why every decision here is made
 * from [TimerSessionRepository.focusBlockingSnapshot] - the durable, per-account SharedPreferences
 * mirror Phase 7 Part 1 introduced - never from the UI-scoped FocusOverlayState/TimerViewModel,
 * which this service never references at all.
 *
 * [FocusBlockingSnapshot.breakEndAtMillis] is deliberately re-evaluated against the current
 * wall-clock time on every single event here, never trusted as a static "on break" flag: if
 * MainActivity/TimerViewModel isn't currently running (the exact condition this service must keep
 * working under), nothing
 * else would ever flip a stale, already-elapsed breakEndAtMillis back to null, so this service
 * would otherwise keep treating an expired break as still active indefinitely. Recomputing from
 * the absolute timestamp on every read is the same pattern TimerSessionSnapshot/FocusOverlayState
 * already use for exactly this reason (see their own "never trust a decrementing counter" doc
 * comments) - applied here, not invented here.
 */
class FocusBlockingAccessibilityService : AccessibilityService() {

    private val repository: TimerSessionRepository by lazy { TimerSessionRepository(applicationContext) }

    /** The last foreground package this service actually evaluated - used only to suppress
     * repeated performGlobalAction(GLOBAL_ACTION_HOME) calls while the same (blocked) package
     * stays in the foreground across multiple window-state-changed events for that one app
     * switch. Reset automatically the moment ANY different package is observed (blocked or not),
     * which is exactly when a fresh evaluation - and a fresh block, if still warranted - is
     * correct again. No timer/polling involved. */
    private var lastForegroundPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return

        if (foregroundPackage == lastForegroundPackage) return
        lastForegroundPackage = foregroundPackage

        // Defensive: One Task must never block itself. Phase 5's own picker already excludes this
        // app's package from every selectable list, so blockedPackages should never contain it -
        // this is a belt-and-suspenders check on top of that, not a substitute for it.
        if (foregroundPackage == packageName) return

        val snapshot = repository.focusBlockingSnapshot()
        if (snapshot.blockedPackages.isEmpty()) return

        val isOnBreak = snapshot.breakEndAtMillis != null && System.currentTimeMillis() < snapshot.breakEndAtMillis
        if (isOnBreak) return

        if (foregroundPackage !in snapshot.blockedPackages) return

        Log.d(TAG, "Blocking $foregroundPackage during Focus - sending user Home")
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        // No ongoing feedback/action for this service to interrupt - performGlobalAction is a
        // one-shot call, not a continuous action that needs cancellation.
    }

    private companion object {
        private const val TAG = "FocusBlockingA11yService"
    }
}
