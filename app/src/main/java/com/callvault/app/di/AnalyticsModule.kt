package com.callvault.app.di

import com.callvault.app.data.analytics.AnalyticsTracker
import com.callvault.app.data.analytics.PostHogTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds @Singleton
    abstract fun bindAnalyticsTracker(impl: PostHogTracker): AnalyticsTracker
}
