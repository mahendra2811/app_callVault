package com.callNest.app.domain.usecase

import com.callNest.app.data.system.NormalizationResult
import com.callNest.app.data.system.PhoneNumberNormalizer
import javax.inject.Inject

/**
 * Thin domain wrapper over [PhoneNumberNormalizer] so call sites depend on
 * the use-case interface rather than the system module directly.
 */
class NormalizePhoneNumberUseCase @Inject constructor(
    private val normalizer: PhoneNumberNormalizer
) {
    operator fun invoke(raw: String?, defaultRegion: String): NormalizationResult =
        normalizer.normalize(raw, defaultRegion)
}
