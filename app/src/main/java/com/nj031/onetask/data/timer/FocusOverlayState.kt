package com.nj031.onetask.data.timer

/** Maximum number of manual breaks a Focus session may be configured with. */
const val MAX_FOCUS_BREAKS = 8

/** Fixed break length - not yet configurable (a future feature, per spec). */
const val FOCUS_BREAK_DURATION_MILLIS = 10 * 60_000L

/**
 * The Timer tab's distraction-blocking Focus Mode session, layered ON TOP of an ordinary running
 * Timer session (TimerSessionRepository/TimerSessionSnapshot) rather than replacing it - a Focus
 * session literally IS a normal Timer countdown (started via TimerViewModel.startTimer, so it
 * inherits the exact same pause/resume/background-survival behavior for free), plus this small
 * bit of extra bookkeeping the normal Timer has no notion of: how many manual breaks are left, and
 * whether one is currently in progress.
 *
 * Deliberately in-memory only (held by TimerViewModel, not persisted to TimerSessionRepository or
 * any other store) - unlike the underlying Timer countdown itself, a Focus session does not need
 * to survive the app process being killed for this phase; it only needs to survive ordinary
 * navigation within a live process, which sharing one TimerViewModel instance between the Timer
 * tab and the Focus Mode Configuration screen already provides.
 */
data class FocusOverlayState(
    val breaksTotal: Int,
    val breaksRemaining: Int,
    /** Non-null only while a manual break is actively counting down (absolute end timestamp) -
     * the underlying Timer is always paused for the exact duration this is non-null. */
    val breakEndAtMillis: Long? = null,
    /** The real Android package names selected in the Block Distractions picker (see
     * FocusBlockedAppsViewModel.selectedPackages) at the moment this session started - carried
     * along for whichever later phase actually enforces blocking against a running session.
     * Phase 6 only transports this value; nothing in this class or TimerViewModel reads it. Set
     * once at session start and never mutated by pause/resume/break/auto-resume - it shares this
     * whole class's in-memory, session-scoped lifecycle (cleared only when the overlay itself is,
     * i.e. on session stop or natural completion). */
    val blockedPackages: Set<String> = emptySet()
) {
    val isOnBreak: Boolean get() = breakEndAtMillis != null

    /** Current remaining break duration, recomputed from the live end timestamp - same
     * never-trust-a-decrementing-counter approach TimerSessionSnapshot itself already uses. */
    fun breakRemainingNowMillis(): Long =
        breakEndAtMillis?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0L
}
