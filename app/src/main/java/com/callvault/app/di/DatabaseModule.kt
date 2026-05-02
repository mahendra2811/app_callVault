package com.callvault.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.callvault.app.data.local.CallVaultDatabase
import com.callvault.app.data.local.dao.AutoTagRuleDao
import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.local.dao.DocFeedbackDao
import com.callvault.app.data.local.dao.FilterPresetDao
import com.callvault.app.data.local.dao.NoteDao
import com.callvault.app.data.local.dao.RuleScoreBoostDao
import com.callvault.app.data.local.dao.SearchHistoryDao
import com.callvault.app.data.local.dao.SkippedUpdateDao
import com.callvault.app.data.local.dao.TagDao
import com.callvault.app.data.local.migration.ALL_MIGRATIONS
import com.callvault.app.data.work.SeedDefaultTagsWorker
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
    fun provideDatabase(@ApplicationContext context: Context): CallVaultDatabase =
        Room.databaseBuilder(
            context,
            CallVaultDatabase::class.java,
            CallVaultDatabase.DATABASE_NAME
        )
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(SeedTagsCallback(context))
            .build()

    @Provides fun provideCallDao(db: CallVaultDatabase): CallDao = db.callDao()
    @Provides fun provideTagDao(db: CallVaultDatabase): TagDao = db.tagDao()
    @Provides fun provideNoteDao(db: CallVaultDatabase): NoteDao = db.noteDao()
    @Provides fun provideContactMetaDao(db: CallVaultDatabase): ContactMetaDao = db.contactMetaDao()
    @Provides fun provideFilterPresetDao(db: CallVaultDatabase): FilterPresetDao = db.filterPresetDao()
    @Provides fun provideAutoTagRuleDao(db: CallVaultDatabase): AutoTagRuleDao = db.autoTagRuleDao()
    @Provides fun provideSearchHistoryDao(db: CallVaultDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun provideDocFeedbackDao(db: CallVaultDatabase): DocFeedbackDao = db.docFeedbackDao()
    @Provides fun provideSkippedUpdateDao(db: CallVaultDatabase): SkippedUpdateDao = db.skippedUpdateDao()
    @Provides fun provideRuleScoreBoostDao(db: CallVaultDatabase): RuleScoreBoostDao = db.ruleScoreBoostDao()
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
