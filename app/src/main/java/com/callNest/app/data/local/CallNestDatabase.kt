package com.callNest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callNest.app.data.local.dao.AutoTagRuleDao
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.DocFeedbackDao
import com.callNest.app.data.local.dao.FilterPresetDao
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.PipelineStageDao
import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.local.dao.SkippedUpdateDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.entity.AutoTagRuleEntity
import com.callNest.app.data.local.entity.RuleScoreBoostEntity
import com.callNest.app.data.local.entity.CallEntity
import com.callNest.app.data.local.entity.CallFts
import com.callNest.app.data.local.entity.CallTagCrossRef
import com.callNest.app.data.local.entity.ContactMetaEntity
import com.callNest.app.data.local.entity.DocFeedbackEntity
import com.callNest.app.data.local.entity.FilterPresetEntity
import com.callNest.app.data.local.entity.NoteEntity
import com.callNest.app.data.local.entity.NoteFts
import com.callNest.app.data.local.entity.NoteHistoryEntity
import com.callNest.app.data.local.entity.PipelineStageEntity
import com.callNest.app.data.local.entity.SearchHistoryEntity
import com.callNest.app.data.local.entity.SkippedUpdateEntity
import com.callNest.app.data.local.entity.TagEntity

/**
 * Room database for callNest. Schema version 1 is the initial release; bump
 * the version and append a `Migration` to
 * [com.callNest.app.data.local.migration.ALL_MIGRATIONS] for any future
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
abstract class CallNestDatabase : RoomDatabase() {

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
        const val DATABASE_NAME = "callNest.db"
    }
}
