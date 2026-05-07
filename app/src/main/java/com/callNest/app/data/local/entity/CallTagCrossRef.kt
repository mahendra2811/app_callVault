package com.callNest.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index

/**
 * Many-to-many bridge between [CallEntity] and [TagEntity].
 *
 * `appliedBy` is `"user"` for manual tags or `"rule:{id}"` for tags
 * applied by an [AutoTagRuleEntity]; the latter form lets us remove
 * orphaned tags when a rule is deleted (spec §8.3).
 */
@Entity(
    tableName = "call_tag_cross_ref",
    primaryKeys = ["callSystemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CallEntity::class,
            parentColumns = ["systemId"],
            childColumns = ["callSystemId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class CallTagCrossRef(
    val callSystemId: Long,
    val tagId: Long,
    val appliedAt: Long = System.currentTimeMillis(),
    val appliedBy: String = "user"
)
