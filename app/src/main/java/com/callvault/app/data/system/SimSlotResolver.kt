package com.callvault.app.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a `PhoneAccountId` to a 0-based SIM slot using
 * [SubscriptionManager].
 *
 * Returns `null` when:
 *  - input is null/blank;
 *  - READ_PHONE_STATE permission is missing (we never throw — surface as
 *    "unknown SIM" so the rest of the sync pipeline keeps moving);
 *  - the subscription is not present.
 */
@Singleton
class SimSlotResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** @return 0-based slot index, or `null` if unknown. */
    fun resolveSimSlot(phoneAccountId: String?): Int? {
        if (phoneAccountId.isNullOrBlank()) return null
        if (!hasPhonePermission()) return null
        val subId = phoneAccountId.toIntOrNull() ?: return null
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? SubscriptionManager ?: return null
            @Suppress("MissingPermission")
            val info = sm.getActiveSubscriptionInfo(subId) ?: return null
            info.simSlotIndex
        } catch (_: SecurityException) {
            null
        }
    }

    /** Carrier display name for the given phone account, or null. */
    fun resolveCarrierName(phoneAccountId: String?): String? {
        if (phoneAccountId.isNullOrBlank()) return null
        if (!hasPhonePermission()) return null
        val subId = phoneAccountId.toIntOrNull() ?: return null
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? SubscriptionManager ?: return null
            @Suppress("MissingPermission")
            sm.getActiveSubscriptionInfo(subId)?.carrierName?.toString()
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasPhonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
}
