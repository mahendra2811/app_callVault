package com.callvault.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Reserved for future top-level providers (clocks, schedulers, etc.).
 * Kept empty in Sprint 1 so the wiring exists for later sprints to extend.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
