package com.callvault.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/** Top-level providers shared across the graph. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Project-wide JSON codec. Tolerant of unknown keys so update manifests
     * and rule-engine payloads stay forward-compatible.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
}
