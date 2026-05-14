package com.callNest.app.di

import com.callNest.app.data.repository.AuthRepositoryImpl
import com.callNest.app.data.repository.AutoTagRuleRepositoryImpl
import com.callNest.app.data.repository.MessageTemplateRepositoryImpl
import com.callNest.app.data.repository.CallRepositoryImpl
import com.callNest.app.data.repository.ContactRepositoryImpl
import com.callNest.app.data.repository.NoteRepositoryImpl
import com.callNest.app.data.repository.SettingsRepositoryImpl
import com.callNest.app.data.repository.TagRepositoryImpl
import com.callNest.app.domain.repository.AuthRepository
import com.callNest.app.domain.repository.AutoTagRuleRepository
import com.callNest.app.domain.repository.MessageTemplateRepository
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.domain.repository.ContactRepository
import com.callNest.app.domain.repository.NoteRepository
import com.callNest.app.domain.repository.SettingsRepository
import com.callNest.app.domain.repository.TagRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindMessageTemplateRepository(impl: MessageTemplateRepositoryImpl): MessageTemplateRepository

    @Binds @Singleton
    abstract fun bindPipelineRepository(
        impl: com.callNest.app.data.repository.PipelineRepositoryImpl
    ): com.callNest.app.domain.repository.PipelineRepository
}
