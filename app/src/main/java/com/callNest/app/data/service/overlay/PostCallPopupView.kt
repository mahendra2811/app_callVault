package com.callNest.app.data.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.callNest.app.MainActivity
import com.callNest.app.data.system.CallContextResolver
import com.callNest.app.domain.model.Note
import com.callNest.app.domain.repository.NoteRepository
import com.callNest.app.domain.repository.TagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Bottom-anchored modal-style overlay shown after a call ends. Uses plain
 * Android views (no Compose) for the same lifecycle simplicity reasons noted
 * in [FloatingBubbleView].
 *
 * Auto-dismisses after `timeoutSec` unless the user interacts with it.
 */
class PostCallPopupView private constructor(context: Context) : FrameLayout(context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun noteRepository(): NoteRepository
        fun tagRepository(): TagRepository
        fun callContextResolver(): CallContextResolver
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var payload: PostCallPayload
    private var remainingMs: Long = 0
    private lateinit var onClose: () -> Unit
    private lateinit var noteField: EditText
    private lateinit var timerLabel: TextView

    companion object {
        fun create(
            context: Context,
            payload: PostCallPayload,
            timeoutSec: Int,
            onClose: () -> Unit
        ): PostCallPopupView {
            val v = PostCallPopupView(context)
            v.payload = payload
            v.remainingMs = timeoutSec * 1000L
            v.onClose = onClose
            v.build()
            v.startCountdown()
            return v
        }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun build() {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            elevation = dp(8).toFloat()
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),
                    dp(24).toFloat(), dp(24).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#FFFFFFFF"))
            }
        }

        // Top row: title + sub on left, X close button on right.
        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(context).apply {
            text = (payload.displayName ?: payload.normalizedNumber)
            setTextColor(Color.parseColor("#FF0F1722"))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val sub = TextView(context).apply {
            text = "${payload.callType} · ${formatDuration(payload.durationSec)}"
            setTextColor(Color.parseColor("#FF5C6A7A"))
            textSize = 13f
            setPadding(0, dp(2), 0, 0)
        }
        titleCol.addView(header)
        titleCol.addView(sub)
        val titleLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        topRow.addView(titleCol, titleLp)

        // X close button — circular grey background, ✕ glyph centred.
        val closeIcon = TextView(context).apply {
            text = "✕"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF5C6A7A"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFF1F1F4"))
            }
            setOnClickListener { dismissAndPersist() }
            contentDescription = "Close"
        }
        topRow.addView(closeIcon, LinearLayout.LayoutParams(dp(36), dp(36)))

        val chipsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val chipsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        }
        timerLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#FF8492A3"))
            textSize = 11f
            setPadding(0, dp(8), 0, 0)
        }
        noteField = EditText(context).apply {
            hint = "Quick note…"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#FFF1F1F4"))
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnFocusChangeListener { _, _ -> resetCountdown() }
        }
        val noteLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        }
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val actionsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        }

        fun pillButton(label: String, primary: Boolean, onTap: () -> Unit): TextView = TextView(context).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            if (primary) {
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#FF0F1722"))
                }
            } else {
                setTextColor(Color.parseColor("#FF0F1722"))
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#FFF1F1F4"))
                }
            }
            setOnClickListener { resetCountdown(); onTap() }
        }

        if (payload.isUnsaved) {
            val saveLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
            actions.addView(pillButton("Save contact", true) { openMainActivityWithNumber() }, saveLp)
        }
        val moreLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        actions.addView(pillButton("More options", false) { openMainActivityWithNumber() }, moreLp)

        // Second action row — WhatsApp + WhatsApp Business. We try the
        // direct package first; fall back to plain wa.me so users without
        // the package don't get a dead button.
        val waRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val waLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        }
        val waButtonLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
        waRow.addView(
            pillButton("WhatsApp", false) {
                openWhatsApp(payload.normalizedNumber, business = false)
            },
            waButtonLp
        )
        val waBLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        waRow.addView(
            pillButton("WA Business", false) {
                openWhatsApp(payload.normalizedNumber, business = true)
            },
            waBLp
        )

        card.addView(topRow)
        card.addView(chipsRow, chipsLp)
        card.addView(noteField, noteLp)
        card.addView(actions, actionsLp)
        card.addView(waRow, waLp)
        card.addView(timerLabel)

        addView(card, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        // Quick-tag chips populated async.
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        scope.launch {
            val top = runCatching {
                deps.callContextResolver().suggestedTagsForNumber(payload.normalizedNumber, 3)
            }.getOrNull().orEmpty()
            for (tag in top) {
                val chip = TextView(context).apply {
                    text = tag.name
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setTextColor(Color.parseColor("#FF2A3441"))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(Color.parseColor("#FFE8E8EC"))
                    }
                    setOnClickListener {
                        resetCountdown()
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                val call = deps.callContextResolver().latestCallForNumber(payload.normalizedNumber)
                                if (call != null) deps.tagRepository().applyTag(call.systemId, tag.id, "popup")
                            }
                        }
                    }
                }
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = dp(6)
                chipsRow.addView(chip, lp)
            }
        }
        setOnTouchListener { _, _ -> resetCountdown(); false }
    }

    private fun startCountdown() {
        scope.launch {
            while (remainingMs > 0 && isAttachedToWindow) {
                timerLabel.text = "Auto-dismiss in ${(remainingMs / 1000)}s"
                delay(250)
                remainingMs -= 250
            }
            if (isAttachedToWindow) dismissAndPersist()
        }
    }

    private fun resetCountdown() {
        // Bump remaining time on any interaction.
        remainingMs = (remainingMs.coerceAtLeast(8_000L))
    }

    private fun dismissAndPersist() {
        val content = noteField.text?.toString().orEmpty().trim()
        if (content.isNotEmpty()) {
            val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val call = deps.callContextResolver().latestCallForNumber(payload.normalizedNumber)
                    val now = Clock.System.now()
                    deps.noteRepository().upsert(
                        Note(
                            id = 0,
                            callSystemId = call?.systemId,
                            normalizedNumber = payload.normalizedNumber,
                            content = content,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                withContext(Dispatchers.Main) { onClose() }
            }
        } else {
            onClose()
        }
    }

    /**
     * Open WhatsApp (or WA Business) directly for [number]. We use the
     * official `wa.me` deep link so users get the right "Open in app" sheet
     * even if both apps are installed, but force the package when
     * [business] is true so business users land in the right inbox.
     */
    private fun openWhatsApp(number: String, business: Boolean) {
        val digits = number.replace("+", "").replace(Regex("\\s"), "")
        val pkg = if (business) "com.whatsapp.w4b" else "com.whatsapp"
        val direct = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$digits"))
            .setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { context.startActivity(direct) }.isSuccess
        if (!opened) {
            // Fallback — no package, let the system picker decide.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$digits"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        onClose()
    }

    private fun openMainActivityWithNumber() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("normalizedNumber", payload.normalizedNumber)
        }
        runCatching { context.startActivity(intent) }
        onClose()
    }

    private fun formatDuration(sec: Int): String =
        "%02d:%02d".format(sec / 60, sec % 60)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
