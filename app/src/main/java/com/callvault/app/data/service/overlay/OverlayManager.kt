package com.callvault.app.data.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.callvault.app.data.event.UiEvent
import com.callvault.app.data.event.UiEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Payload passed from [com.callvault.app.data.service.CallEnrichmentService]
 * to [OverlayManager.showPostCallPopup].
 */
data class PostCallPayload(
    val normalizedNumber: String,
    val displayName: String?,
    val durationSec: Int,
    val callType: String,
    val isUnsaved: Boolean
)

/**
 * Adds and removes overlay views via [WindowManager].
 *
 * Permission-safe: if `Settings.canDrawOverlays` is `false`, every show()
 * silently no-ops and emits a single explanatory snackbar via [UiEventBus].
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uiEventBus: UiEventBus
) {

    private val wm: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var bubble: View? = null
    private var popup: View? = null

    @Synchronized
    fun showBubble(payload: PostCallPayload) {
        if (!ensurePermission()) return
        hideBubble()
        runCatching {
            val view = FloatingBubbleView.create(context, payload) { hideBubble() }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 24
                y = 240
            }
            view.attachDragHandler(wm, params)
            wm.addView(view, params)
            bubble = view
        }.onFailure { Timber.w(it, "Failed to add bubble overlay") }
    }

    @Synchronized
    fun hideBubble() {
        bubble?.let { v ->
            runCatching { wm.removeView(v) }
            bubble = null
        }
    }

    @Synchronized
    fun showPostCallPopup(payload: PostCallPayload, timeoutSec: Int) {
        if (!ensurePermission()) return
        hidePostCallPopup()
        runCatching {
            val view = PostCallPopupView.create(context, payload, timeoutSec) { hidePostCallPopup() }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
            wm.addView(view, params)
            popup = view
        }.onFailure { Timber.w(it, "Failed to add post-call popup") }
    }

    @Synchronized
    fun hidePostCallPopup() {
        popup?.let { v ->
            runCatching { wm.removeView(v) }
            popup = null
        }
    }

    @Synchronized
    fun hideAll() {
        hideBubble()
        hidePostCallPopup()
    }

    fun hasAnyOverlayVisible(): Boolean = bubble != null || popup != null

    private fun ensurePermission(): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            uiEventBus.emit(
                UiEvent.SnackbarWithAction(
                    message = "Allow CallVault to display over other apps to use real-time features.",
                    actionLabel = "Open Settings",
                    actionKey = "open_overlay_settings"
                )
            )
            return false
        }
        return true
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
}
