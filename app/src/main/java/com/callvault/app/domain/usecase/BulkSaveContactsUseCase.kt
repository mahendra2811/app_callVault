package com.callvault.app.domain.usecase

import com.callvault.app.domain.model.Call
import javax.inject.Inject

/**
 * Sprint 5 — bulk auto-save invoked from the Inquiries screen "Save all"
 * action. Iterates over [unsavedCalls], delegating each to
 * [AutoSaveContactUseCase], and publishes progress through
 * [BulkSaveProgressBus] so the UI can render a determinate dialog.
 *
 * Errors from individual saves are kept local — the loop never aborts —
 * and the first error message is reported via [BulkSaveProgress.Done].
 */
class BulkSaveContactsUseCase @Inject constructor(
    private val autoSave: AutoSaveContactUseCase,
    private val progressBus: BulkSaveProgressBus
) {

    /**
     * @return `Triple(savedCount, skippedCount, firstError)`.
     */
    suspend operator fun invoke(unsavedCalls: List<Call>): Triple<Int, Int, String?> {
        val total = unsavedCalls.size
        if (total == 0) {
            progressBus.publish(BulkSaveProgress.Done(0, 0, 0, null))
            return Triple(0, 0, null)
        }
        var saved = 0
        var skipped = 0
        var firstError: String? = null

        progressBus.publish(BulkSaveProgress.Running(current = 0, total = total))
        unsavedCalls.forEachIndexed { idx, call ->
            when (val res = autoSave(call)) {
                is AutoSaveContactUseCase.Result.Saved -> saved++
                is AutoSaveContactUseCase.Result.Failed -> {
                    skipped++
                    if (firstError == null) firstError = res.reason
                }
                else -> skipped++
            }
            progressBus.publish(
                BulkSaveProgress.Running(
                    current = idx + 1,
                    total = total,
                    lastError = firstError
                )
            )
        }
        progressBus.publish(
            BulkSaveProgress.Done(
                savedCount = saved,
                skippedCount = skipped,
                totalCount = total,
                firstError = firstError
            )
        )
        return Triple(saved, skipped, firstError)
    }
}
