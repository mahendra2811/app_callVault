package com.callvault.app.data.system

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 * libphonenumber-android wrapper that produces an E.164 form plus
 * country-ISO metadata suitable for storage.
 *
 * Edge cases per spec §13:
 *  - Empty / blank / withheld numbers → [NormalizationResult.private]
 *  - Short codes (<=5 digits) — kept raw, no E.164, no country.
 *  - International numbers we can't parse — keep raw, country = null.
 */
@Singleton
class PhoneNumberNormalizer @Inject constructor(
    @ApplicationContext context: Context
) {
    private val util: PhoneNumberUtil = PhoneNumberUtil.createInstance(context)

    /**
     * @param raw The user-visible / call log number (may be `null`/empty).
     * @param defaultRegion ISO 3166-1 alpha-2 region code, e.g. `"IN"`.
     */
    fun normalize(raw: String?, defaultRegion: String): NormalizationResult {
        if (raw.isNullOrBlank()) return NormalizationResult.private()
        val trimmed = raw.trim()

        // Blocked / private / unknown markers
        if (trimmed in BLOCKED_MARKERS) return NormalizationResult.private()

        // Short codes (5 digits or fewer) — keep raw, skip parsing.
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length in 1..5) {
            return NormalizationResult(
                e164 = null,
                countryIso = null,
                isPrivate = false
            )
        }

        return try {
            val parsed = util.parse(trimmed, defaultRegion)
            if (!util.isValidNumber(parsed)) {
                NormalizationResult(e164 = null, countryIso = null, isPrivate = false)
            } else {
                val e164 = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                val region = util.getRegionCodeForNumber(parsed)
                NormalizationResult(
                    e164 = e164,
                    countryIso = region,
                    isPrivate = false
                )
            }
        } catch (_: NumberParseException) {
            NormalizationResult(e164 = null, countryIso = null, isPrivate = false)
        }
    }

    private companion object {
        // Common sentinels for hidden-caller-id rows in CallLog.
        val BLOCKED_MARKERS = setOf("-1", "-2", "-3", "private", "unknown", "anonymous", "withheld")
    }
}

/**
 * Result of [PhoneNumberNormalizer.normalize].
 *
 * @property e164 The fully-qualified `+CC...` form, or `null` if parsing
 *               failed / the number is private / it's a short code.
 * @property countryIso ISO 3166-1 alpha-2 country code or `null`.
 * @property isPrivate `true` when the source row had no usable number at
 *                    all (withheld / hidden caller ID).
 */
data class NormalizationResult(
    val e164: String?,
    val countryIso: String?,
    val isPrivate: Boolean
) {
    companion object {
        fun private(): NormalizationResult =
            NormalizationResult(e164 = null, countryIso = null, isPrivate = true)
    }
}
