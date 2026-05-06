package com.callvault.app.data.analytics

import android.app.Application
import com.callvault.app.BuildConfig
import com.callvault.app.domain.model.AuthState
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Lightweight analytics surface; UI/VMs depend on this, not PostHog directly. */
interface AnalyticsTracker {
    fun init(app: Application)
    fun track(event: String, props: Map<String, Any?> = emptyMap())
    fun screen(name: String, props: Map<String, Any?> = emptyMap())
    fun identifyOnSignIn(state: AuthState)
    fun reset()
}

@Singleton
class PostHogTracker @Inject constructor() : AnalyticsTracker {

    private var initialized = false

    override fun init(app: Application) {
        if (initialized) return
        if (BuildConfig.POSTHOG_API_KEY.isBlank()) {
            Timber.w("PostHog API key missing — analytics disabled. Set POSTHOG_API_KEY in local.properties.")
            return
        }
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            captureScreenViews = true
            captureDeepLinks = true
            sessionReplay = false
            debug = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(app, config)
        initialized = true
    }

    override fun track(event: String, props: Map<String, Any?>) {
        if (initialized) PostHog.capture(event = event, properties = props.filterNonNull())
    }

    override fun screen(name: String, props: Map<String, Any?>) {
        if (initialized) PostHog.screen(screenTitle = name, properties = props.filterNonNull())
    }

    private fun Map<String, Any?>.filterNonNull(): Map<String, Any> =
        mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    override fun identifyOnSignIn(state: AuthState) {
        if (!initialized) return
        if (state is AuthState.SignedIn) {
            PostHog.identify(
                distinctId = state.session.userId,
                userProperties = buildMap {
                    state.session.email?.let { put("email", it) }
                    state.session.displayName?.let { put("name", it) }
                },
            )
        }
    }

    override fun reset() {
        if (initialized) PostHog.reset()
    }
}
