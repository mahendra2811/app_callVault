package com.callvault.app.data.service.overlay

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
import com.callvault.app.MainActivity
import com.callvault.app.data.system.CallContextResolver
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.repository.NoteRepository
import com.callvault.app.domain.repository.TagRepository
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
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    dp(20).toFloat(), dp(20).toFloat(),
                    dp(20).toFloat(), dp(20).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#FFFFFFFF"))
            }
        }
        val header = TextView(context).apply {
            text = (payload.displayName ?: payload.normalizedNumber)
            setTextColor(Color.parseColor("#FF2A3441"))
            textSize = 18f
        }
        val sub = TextView(context).apply {
            text = "${payload.callType} · ${formatDuration(payload.durationSec)}"
            setTextColor(Color.parseColor("#FF5C6A7A"))
            textSize = 13f
        }
        val chipsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        timerLabel = TextView(context).apply {
            setTextColor(Color.parseColor("#FF8492A3"))
            textSize = 12f
        }
        noteField = EditText(context).apply {
            hint = "Quick note…"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            setBackgroundColor(Color.parseColor("#FFF1F1F4"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnFocusChangeListener { _, _ -> resetCountdown() }
        }
        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        val saveContact = TextView(context).apply {
            text = "Save contact"
            setTextColor(Color.parseColor("#FF2A3441"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener {
                resetCountdown()
                openMainActivityWithNumber()
            }
        }
        val moreOptions = TextView(context).apply {
            text = "More options"
            setTextColor(Color.parseColor("#FF2A3441"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener {
                resetCountdown()
                openMainActivityWithNumber()
            }
        }
        val closeBtn = TextView(context).apply {
            text = "Close"
            setTextColor(Color.parseColor("#FF2A3441"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { dismissAndPersist() }
        }
        if (payload.isUnsaved) actions.addView(saveContact)
        actions.addView(moreOptions)
        actions.addView(closeBtn)

        card.addView(header)
        card.addView(sub)
        card.addView(chipsRow)
        card.addView(noteField, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        card.addView(actions)
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
