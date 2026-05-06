package com.callvault.app.data.local.mapper

import com.callvault.app.data.local.entity.AutoTagRuleEntity
import com.callvault.app.data.local.entity.CallEntity
import com.callvault.app.data.local.entity.ContactMetaEntity
import com.callvault.app.data.local.entity.NoteEntity
import com.callvault.app.data.local.entity.TagEntity
import com.callvault.app.domain.model.AutoTagRule
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.CallType
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.model.RuleAction
import com.callvault.app.domain.model.RuleCondition
import com.callvault.app.domain.model.Tag
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Single shared JSON instance — class-discriminator default works for sealed interfaces. */
val ruleJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

private fun Long.asInstant(): Instant = Instant.fromEpochMilliseconds(this)
private fun Long?.asInstantOrNull(): Instant? = this?.let(Instant::fromEpochMilliseconds)
private fun Instant.toMillis(): Long = toEpochMilliseconds()

// ---------- Calls ----------

fun CallEntity.toDomain(): Call = Call(
    systemId = systemId,
    rawNumber = rawNumber,
    normalizedNumber = normalizedNumber,
    date = date.asInstant(),
    durationSec = duration,
    type = CallType.fromRaw(type),
    cachedName = cachedName,
    phoneAccountId = phoneAccountId,
    simSlot = simSlot,
    carrierName = carrierName,
    geocodedLocation = geocodedLocation,
    countryIso = countryIso,
    isNew = isNew,
    isBookmarked = isBookmarked,
    bookmarkReason = bookmarkReason,
    followUpAt = followUpDate.asInstantOrNull(),
    followUpMinuteOfDay = followUpTime,
    followUpNote = followUpNote,
    followUpDoneAt = followUpDoneAt.asInstantOrNull(),
    leadScore = leadScore,
    leadScoreManualOverride = leadScoreManualOverride,
    isArchived = isArchived,
    deletedAt = deletedAt.asInstantOrNull(),
    createdAt = createdAt.asInstant(),
    updatedAt = updatedAt.asInstant()
)

fun Call.toEntity(): CallEntity = CallEntity(
    systemId = systemId,
    rawNumber = rawNumber,
    normalizedNumber = normalizedNumber,
    date = date.toMillis(),
    duration = durationSec,
    type = type.raw,
    cachedName = cachedName,
    phoneAccountId = phoneAccountId,
    simSlot = simSlot,
    carrierName = carrierName,
    geocodedLocation = geocodedLocation,
    countryIso = countryIso,
    isNew = isNew,
    isBookmarked = isBookmarked,
    bookmarkReason = bookmarkReason,
    followUpDate = followUpAt?.toMillis(),
    followUpTime = followUpMinuteOfDay,
    followUpNote = followUpNote,
    followUpDoneAt = followUpDoneAt?.toMillis(),
    leadScore = leadScore,
    leadScoreManualOverride = leadScoreManualOverride,
    isArchived = isArchived,
    deletedAt = deletedAt?.toMillis(),
    createdAt = createdAt.toMillis(),
    updatedAt = updatedAt.toMillis()
)

// ---------- Tags ----------

fun TagEntity.toDomain(): Tag = Tag(id, name, colorHex, emoji, isSystem, sortOrder, whatsappTemplate)
fun Tag.toEntity(): TagEntity = TagEntity(
    id = id, name = name, colorHex = colorHex, emoji = emoji,
    isSystem = isSystem, sortOrder = sortOrder, whatsappTemplate = whatsappTemplate
)

// ---------- Notes ----------

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    callSystemId = callSystemId,
    normalizedNumber = normalizedNumber,
    content = content,
    createdAt = createdAt.asInstant(),
    updatedAt = updatedAt.asInstant()
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    callSystemId = callSystemId,
    normalizedNumber = normalizedNumber,
    content = content,
    createdAt = createdAt.toMillis(),
    updatedAt = updatedAt.toMillis()
)

// ---------- ContactMeta ----------

fun ContactMetaEntity.toDomain(): ContactMeta = ContactMeta(
    normalizedNumber = normalizedNumber,
    displayName = displayName,
    isInSystemContacts = isInSystemContacts,
    systemContactId = systemContactId,
    systemRawContactId = systemRawContactId,
    isAutoSaved = isAutoSaved,
    autoSavedAt = autoSavedAt.asInstantOrNull(),
    autoSavedFormat = autoSavedFormat,
    firstCallDate = firstCallDate.asInstant(),
    lastCallDate = lastCallDate.asInstant(),
    totalCalls = totalCalls,
    totalDuration = totalDuration,
    incomingCount = incomingCount,
    outgoingCount = outgoingCount,
    missedCount = missedCount,
    computedLeadScore = computedLeadScore,
    source = source,
    updatedAt = updatedAt.asInstant()
)

fun ContactMeta.toEntity(): ContactMetaEntity = ContactMetaEntity(
    normalizedNumber = normalizedNumber,
    displayName = displayName,
    isInSystemContacts = isInSystemContacts,
    systemContactId = systemContactId,
    systemRawContactId = systemRawContactId,
    isAutoSaved = isAutoSaved,
    autoSavedAt = autoSavedAt?.toMillis(),
    autoSavedFormat = autoSavedFormat,
    firstCallDate = firstCallDate.toMillis(),
    lastCallDate = lastCallDate.toMillis(),
    totalCalls = totalCalls,
    totalDuration = totalDuration,
    incomingCount = incomingCount,
    outgoingCount = outgoingCount,
    missedCount = missedCount,
    computedLeadScore = computedLeadScore,
    source = source,
    updatedAt = updatedAt.toMillis()
)

// ---------- AutoTagRule ----------

fun AutoTagRuleEntity.toDomain(): AutoTagRule = AutoTagRule(
    id = id,
    name = name,
    isActive = isActive,
    sortOrder = sortOrder,
    conditions = ruleJson.decodeFromString(
        ListSerializer(RuleCondition.serializer()),
        conditionsJson
    ),
    actions = ruleJson.decodeFromString(
        ListSerializer(RuleAction.serializer()),
        actionsJson
    ),
    createdAt = createdAt.asInstant()
)

fun AutoTagRule.toEntity(): AutoTagRuleEntity = AutoTagRuleEntity(
    id = id,
    name = name,
    isActive = isActive,
    sortOrder = sortOrder,
    conditionsJson = ruleJson.encodeToString(
        ListSerializer(RuleCondition.serializer()),
        conditions
    ),
    actionsJson = ruleJson.encodeToString(
        ListSerializer(RuleAction.serializer()),
        actions
    ),
    createdAt = createdAt.toMillis()
)
