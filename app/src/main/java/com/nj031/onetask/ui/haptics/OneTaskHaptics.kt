package com.nj031.onetask.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Whether General Settings > Haptic Feedback is turned on - provided once near the root of the
 * app (see OneTaskNavHost) so any screen can gate a haptic call without threading a boolean
 * parameter through every composable in between. Defaults to true (the spec's default) for any
 * composable rendered outside that provider, e.g. a @Preview.
 */
val LocalHapticFeedbackEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

/**
 * A short, subtle tick for a genuinely meaningful interaction (task completion, timer controls,
 * date selection, an important toggle or confirmation) - deliberately not used for routine taps,
 * scrolling, typing, or ordinary navigation. Uses Compose's own LocalHapticFeedback (the
 * platform's standard haptic mechanism) rather than a custom Vibrator-based system, and is a
 * silent no-op whenever the user has turned the setting off.
 */
@Composable
fun rememberHapticTick(): () -> Unit {
    val enabled = LocalHapticFeedbackEnabled.current
    val haptics = LocalHapticFeedback.current
    return remember(enabled, haptics) {
        {
            if (enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}
