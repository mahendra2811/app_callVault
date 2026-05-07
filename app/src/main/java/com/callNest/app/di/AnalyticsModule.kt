package com.callNest.app.di

import com.callNest.app.data.analytics.AnalyticsTracker
import com.callNest.app.data.analytics.PostHogTracker
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
