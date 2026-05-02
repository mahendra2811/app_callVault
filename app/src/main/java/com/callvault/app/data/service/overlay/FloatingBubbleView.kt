package com.callvault.app.data.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.repository.NoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Programmatic Android view used as the in-call floating bubble.
 *
 * Implemented as a plain [FrameLayout] (not a `ComposeView`) to avoid the
 * non-trivial set-up of `ViewTree*Owner` fakes inside an overlay window.
 * The ergonomics are simpler and the surface is small enough that Compose
 * adds no value here. See `DECISIONS.md` "Compose-in-overlay" entry.
 */
class FloatingBubbleView private constructor(context: Context) : FrameLayout(context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun noteRepository(): NoteRepository
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var startMs: Long = SystemClock.elapsedRealtime()
    private lateinit var payload: PostCallPayload
    private lateinit var onClose: () -> Unit
    private var expanded = false
    private var bubble: View? = null
    private var card: View? = null
    private val noteState: StringBuilder = StringBuilder()

    companion object {
        fun create(context: Context, payload: PostCallPayload, onClose: () -> Unit): FloatingBubbleView {
            val v = FloatingBubbleView(context)
            v.payload = payload
            v.onClose = onClose
            v.buildCollapsed()
            return v
        }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun buildCollapsed() {
        removeAllViews()
        val size = dp(56)
        val tv = TextView(context).apply {
            text = (payload.displayName?.firstOrNull()?.uppercase() ?: "C")
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF2A3441"))
            }
        }
        val lp = LayoutParams(size, size)
        addView(tv, lp)
        bubble = tv
        setOnClickListener { if (!expanded) expand() else collapse() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun expand() {
        expanded = true
        removeAllViews()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#FFFFFFFF"))
            }
        }
        val title = TextView(context).apply {
            text = payload.displayName ?: payload.normalizedNumber
            setTextColor(Color.parseColor("#FF2A3441"))
            textSize = 16f
        }
        val timer = TextView(context).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#FF5C6A7A"))
            textSize = 13f
        }
        val noteField = EditText(context).apply {
            hint = "Note this call…"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false)
            minLines = 2
            maxLines = 4
            setBackgroundColor(Color.parseColor("#FFF1F1F4"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val close = TextView(context).apply {
            text = "Close"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setTextColor(Color.parseColor("#FF2A3441"))
            setOnClickListener {
                noteState.clear().append(noteField.text?.toString().orEmpty())
                persistNoteIfAny()
                onClose()
            }
        }
        actions.addView(close)
        container.addView(title)
        container.addView(timer)
        container.addView(noteField, LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(actions)
        addView(container, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        card = container

        scope.launch {
            while (isAttachedToWindow && expanded) {
                val sec = ((SystemClock.elapsedRealtime() - startMs) / 1000L).toInt()
                timer.text = "%02d:%02d".format(sec / 60, sec % 60)
                delay(1000)
            }
        }
    }

    private fun collapse() {
        expanded = false
        buildCollapsed()
    }

    private fun persistNoteIfAny() {
        val content = noteState.toString().trim()
        if (content.isEmpty()) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val repo = deps.noteRepository()
        val now = Clock.System.now()
        val note = Note(
            id = 0,
            callSystemId = null,
            normalizedNumber = payload.normalizedNumber,
            content = content,
            createdAt = now,
            updatedAt = now
        )
        scope.launch(Dispatchers.IO) { runCatching { repo.upsert(note) } }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    /** Drag handler that snaps the bubble to the nearest screen edge on release. */
    @SuppressLint("ClickableViewAccessibility")
    fun attachDragHandler(wm: WindowManager, params: WindowManager.LayoutParams) {
        var initX = 0; var initY = 0; var touchX = 0f; var touchY = 0f
        var dragged = false
        setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = ev.rawX; touchY = ev.rawY; dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - touchX).toInt(); val dy = (ev.rawY - touchY).toInt()
                    if (dx * dx + dy * dy > 64) dragged = true
                    params.x = initX + dx; params.y = initY + dy
                    runCatching { wm.updateViewLayout(this, params) }; true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        // Snap to nearest horizontal edge.
                        val screenW = resources.displayMetrics.widthPixels
                        params.x = if (params.x + width / 2 < screenW / 2) 16 else max(16, screenW - width - 16)
                        runCatching { wm.updateViewLayout(this, params) }
                        true
                    } else {
                        performClick(); true
                    }
                }
                else -> false
            }
        }
    }
}
