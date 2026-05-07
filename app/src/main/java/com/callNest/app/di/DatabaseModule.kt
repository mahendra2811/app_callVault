package com.callNest.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.callNest.app.data.local.CallNestDatabase
import com.callNest.app.data.local.dao.AutoTagRuleDao
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.DocFeedbackDao
import com.callNest.app.data.local.dao.FilterPresetDao
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.local.dao.SkippedUpdateDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.migration.ALL_MIGRATIONS
import com.callNest.app.data.work.SeedDefaultTagsWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallNestDatabase =
        Room.databaseBuilder(
            context,
            CallNestDatabase::class.java,
            CallNestDatabase.DATABASE_NAME
        )
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(SeedTagsCallback(context))
            .build()

    @Provides fun provideCallDao(db: CallNestDatabase): CallDao = db.callDao()
    @Provides fun provideTagDao(db: CallNestDatabase): TagDao = db.tagDao()
    @Provides fun provideNoteDao(db: CallNestDatabase): NoteDao = db.noteDao()
    @Provides fun provideContactMetaDao(db: CallNestDatabase): ContactMetaDao = db.contactMetaDao()
    @Provides fun provideFilterPresetDao(db: CallNestDatabase): FilterPresetDao = db.filterPresetDao()
    @Provides fun provideAutoTagRuleDao(db: CallNestDatabase): AutoTagRuleDao = db.autoTagRuleDao()
    @Provides fun provideSearchHistoryDao(db: CallNestDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun provideDocFeedbackDao(db: CallNestDatabase): DocFeedbackDao = db.docFeedbackDao()
    @Provides fun provideSkippedUpdateDao(db: CallNestDatabase): SkippedUpdateDao = db.skippedUpdateDao()
    @Provides fun provideRuleScoreBoostDao(db: CallNestDatabase): RuleScoreBoostDao = db.ruleScoreBoostDao()
    @Provides fun providePipelineStageDao(db: CallNestDatabase): com.callNest.app.data.local.dao.PipelineStageDao = db.pipelineStageDao()
}

/**
 * Room callback that runs once after database creation and enqueues
 * [SeedDefaultTagsWorker] via WorkManager — keeps the seed off the main
 * thread without blocking Room's `onCreate`.
 */
private class SeedTagsCallback(
    private val context: Context
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val req = OneTimeWorkRequestBuilder<SeedDefaultTagsWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SeedDefaultTagsWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }
}
