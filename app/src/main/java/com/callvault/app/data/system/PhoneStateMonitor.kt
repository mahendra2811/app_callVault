package com.callvault.app.data.system

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Real-time bridge over the Android telephony stack.
 *
 * - On API 31+ uses [TelephonyCallback.CallStateListener].
 * - On API 26-30 falls back to the legacy [PhoneStateListener.LISTEN_CALL_STATE].
 *
 * Because [TelephonyCallback] does not expose the incoming number directly,
 * we additionally register a `BroadcastReceiver` for
 * `android.intent.action.PHONE_STATE` to capture `EXTRA_INCOMING_NUMBER`
 * (requires `READ_CALL_LOG`). When that permission is missing the flow still
 * emits state transitions but with a `null` number — overlays handle that
 * gracefully.
 */
@Singleton
class PhoneStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** High-level call lifecycle states emitted by [observe]. */
    sealed class CallState {
        data object Idle : CallState()
        data class Ringing(val number: String?) : CallState()
        data class Offhook(val number: String?) : CallState()
    }

    /** Cold flow of [CallState]; one active subscription per service. */
    fun observe(): Flow<CallState> = callbackFlow {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (tm == null) {
            trySend(CallState.Idle)
            awaitClose { }
            return@callbackFlow
        }

        // Latest captured number from the broadcast — TelephonyCallback path can't get it directly.
        val lastNumberRef = java.util.concurrent.atomic.AtomicReference<String?>(null)

        val numberReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val n = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    if (!n.isNullOrBlank()) lastNumberRef.set(n)
                }
            }
        }
        val canReceiveNumber = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (canReceiveNumber) {
            try {
                ContextCompat.registerReceiver(
                    context,
                    numberReceiver,
                    IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                    ContextCompat.RECEIVER_EXPORTED
                )
            } catch (t: Throwable) {
                Timber.w(t, "PhoneStateMonitor: receiver registration failed")
            }
        }

        fun mapAndSend(state: Int, legacyNumber: String?) {
            val number = legacyNumber?.takeIf { it.isNotBlank() } ?: lastNumberRef.get()
            val mapped = when (state) {
                TelephonyManager.CALL_STATE_RINGING -> CallState.Ringing(number)
                TelephonyManager.CALL_STATE_OFFHOOK -> CallState.Offhook(number)
                else -> CallState.Idle
            }
            trySend(mapped)
        }

        var modernCallback: TelephonyCallback? = null
        var legacyListener: PhoneStateListener? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = mapAndSend(state, null)
            }
            modernCallback = cb
            try {
                tm.registerTelephonyCallback(context.mainExecutor, cb)
            } catch (t: Throwable) {
                Timber.w(t, "registerTelephonyCallback failed")
            }
        } else {
            val l = object : PhoneStateListener() {
                @Deprecated("Pre-31 API")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                    mapAndSend(state, phoneNumber)
            }
            legacyListener = l
            try {
                @Suppress("DEPRECATION")
                tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (t: Throwable) {
                Timber.w(t, "PhoneStateListener registration failed")
            }
        }

        // Seed an Idle so consumers can start their state machine.
        trySend(CallState.Idle)

        awaitClose {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && modernCallback != null) {
                    tm.unregisterTelephonyCallback(modernCallback)
                } else if (legacyListener != null) {
                    @Suppress("DEPRECATION")
                    tm.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
                }
            } catch (_: Throwable) { /* ignore */ }
            if (canReceiveNumber) {
                try { context.unregisterReceiver(numberReceiver) } catch (_: Throwable) {}
            }
        }
    }.distinctUntilChanged()
}

/** Legacy alias kept for code that still references the old enum-style events. */
sealed interface PhoneCallEvent {
    data class Ringing(val rawNumber: String?) : PhoneCallEvent
    data class OffHook(val rawNumber: String?) : PhoneCallEvent
    data class Idle(val lastNumber: String?) : PhoneCallEvent
}
