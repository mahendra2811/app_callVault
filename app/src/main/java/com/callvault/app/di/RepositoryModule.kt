package com.callvault.app.di

import com.callvault.app.data.repository.AuthRepositoryImpl
import com.callvault.app.data.repository.AutoTagRuleRepositoryImpl
import com.callvault.app.data.repository.MessageTemplateRepositoryImpl
import com.callvault.app.data.repository.CallRepositoryImpl
import com.callvault.app.data.repository.ContactRepositoryImpl
import com.callvault.app.data.repository.NoteRepositoryImpl
import com.callvault.app.data.repository.SettingsRepositoryImpl
import com.callvault.app.data.repository.TagRepositoryImpl
import com.callvault.app.data.repository.UpdateRepositoryImpl
import com.callvault.app.domain.repository.AuthRepository
import com.callvault.app.domain.repository.AutoTagRuleRepository
import com.callvault.app.domain.repository.MessageTemplateRepository
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.NoteRepository
import com.callvault.app.domain.repository.SettingsRepository
import com.callvault.app.domain.repository.TagRepository
import com.callvault.app.domain.repository.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindCallRepository(impl: CallRepositoryImpl): CallRepository

    @Binds @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds @Singleton
    abstract fun bindAutoTagRuleRepository(
        impl: AutoTagRuleRepositoryImpl
    ): AutoTagRuleRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindMessageTemplateRepository(impl: MessageTemplateRepositoryImpl): MessageTemplateRepository

    @Binds @Singleton
    abstract fun bindPipelineRepository(
        impl: com.callvault.app.data.repository.PipelineRepositoryImpl
    ): com.callvault.app.domain.repository.PipelineRepository
}
