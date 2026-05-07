package com.callNest.app.domain.usecase

import com.callNest.app.data.system.ContactsReader
import javax.inject.Inject

/**
 * Resolves the live display name for a normalized number. Wraps
 * [ContactsReader] so callers don't depend on Android types.
 */
class ResolveContactUseCase @Inject constructor(
    private val reader: ContactsReader
) {
    /** @return the display name, or `null` if not found / no permission. */
    suspend operator fun invoke(normalizedNumber: String): String? =
        reader.resolveDisplayName(normalizedNumber)
}
