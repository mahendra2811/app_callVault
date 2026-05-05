package com.callvault.app.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView

/**
 * Phase II — Haptic tokens.
 *
 * Maps to UI-spec §11. Every primary action ships with a calibrated haptic.
 * Call sites are wired in Phase II.5/II.6 sweeps.
 */
object Haptics {
    /** Light tick — list-row tap. */
    fun light(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)

    /** Long-press / destructive confirm. */
    fun medium(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

    /** Switch toggle. */
    fun tick(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

    /** Filter applied / chip click. */
    fun click(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

    /** FAB long-press / save success. */
    fun heavy(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    /** Pull-to-refresh threshold. */
    fun doubleClick(view: View) =
        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
}

/**
 * Compose helper — returns a callback that routes Compose's [HapticFeedbackType]
 * to the underlying View's haptic constants.
 */
@Composable
fun rememberHapticPerformer(): (HapticFeedbackType) -> Unit {
    val view = LocalView.current
    return { type ->
        when (type) {
            HapticFeedbackType.LongPress ->
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            else ->
                view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }
}
