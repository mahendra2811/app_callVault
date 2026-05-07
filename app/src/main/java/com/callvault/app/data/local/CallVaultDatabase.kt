package com.callvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callvault.app.data.local.dao.AutoTagRuleDao
import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.local.dao.DocFeedbackDao
import com.callvault.app.data.local.dao.FilterPresetDao
import com.callvault.app.data.local.dao.NoteDao
import com.callvault.app.data.local.dao.PipelineStageDao
import com.callvault.app.data.local.dao.RuleScoreBoostDao
import com.callvault.app.data.local.dao.SearchHistoryDao
import com.callvault.app.data.local.dao.SkippedUpdateDao
import com.callvault.app.data.local.dao.TagDao
import com.callvault.app.data.local.entity.AutoTagRuleEntity
import com.callvault.app.data.local.entity.RuleScoreBoostEntity
import com.callvault.app.data.local.entity.CallEntity
import com.callvault.app.data.local.entity.CallFts
import com.callvault.app.data.local.entity.CallTagCrossRef
import com.callvault.app.data.local.entity.ContactMetaEntity
import com.callvault.app.data.local.entity.DocFeedbackEntity
import com.callvault.app.data.local.entity.FilterPresetEntity
import com.callvault.app.data.local.entity.NoteEntity
import com.callvault.app.data.local.entity.NoteFts
import com.callvault.app.data.local.entity.NoteHistoryEntity
import com.callvault.app.data.local.entity.PipelineStageEntity
import com.callvault.app.data.local.entity.SearchHistoryEntity
import com.callvault.app.data.local.entity.SkippedUpdateEntity
import com.callvault.app.data.local.entity.TagEntity

/**
 * Room database for CallVault. Schema version 1 is the initial release; bump
 * the version and append a `Migration` to
 * [com.callvault.app.data.local.migration.ALL_MIGRATIONS] for any future
 * changes.
 */
@Database(
    version = 4,
    exportSchema = true,
    entities = [
        CallEntity::class,
        TagEntity::class,
        CallTagCrossRef::class,
        ContactMetaEntity::class,
        NoteEntity::class,
        NoteHistoryEntity::class,
        FilterPresetEntity::class,
        AutoTagRuleEntity::class,
        RuleScoreBoostEntity::class,
        SearchHistoryEntity::class,
        DocFeedbackEntity::class,
        SkippedUpdateEntity::class,
        CallFts::class,
        NoteFts::class,
        PipelineStageEntity::class
    ]
)
abstract class CallVaultDatabase : RoomDatabase() {

    abstract fun callDao(): CallDao
    abstract fun tagDao(): TagDao
    abstract fun noteDao(): NoteDao
    abstract fun contactMetaDao(): ContactMetaDao
    abstract fun filterPresetDao(): FilterPresetDao
    abstract fun autoTagRuleDao(): AutoTagRuleDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun docFeedbackDao(): DocFeedbackDao
    abstract fun skippedUpdateDao(): SkippedUpdateDao
    abstract fun ruleScoreBoostDao(): RuleScoreBoostDao
    abstract fun pipelineStageDao(): PipelineStageDao

    companion object {
        const val DATABASE_NAME = "callvault.db"
    }
}
