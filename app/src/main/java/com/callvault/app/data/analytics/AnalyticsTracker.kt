package com.callvault.app.data.analytics

import android.app.Application
import com.callvault.app.BuildConfig
import com.callvault.app.domain.model.AuthState
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Lightweight analytics surface; UI/VMs depend on this, not PostHog directly. */
interface AnalyticsTracker {
    fun init(app: Application)
    fun setConsent(granted: Boolean)
    fun track(event: String, props: Map<String, Any?> = emptyMap())
    fun screen(name: String, props: Map<String, Any?> = emptyMap())
    fun identifyOnSignIn(state: AuthState)
    fun reset()
}

/**
 * PostHog implementation. Initialization is consent-gated — `init()` does NOT contact PostHog
 * until [setConsent] is called with `true`. PII (email, name) is never sent; only the Supabase
 * userId is used as `distinctId`.
 */
@Singleton
class PostHogTracker @Inject constructor() : AnalyticsTracker {

    private val initialized = AtomicBoolean(false)
    private var app: Application? = null

    override fun init(app: Application) {
        this.app = app
    }

    override fun setConsent(granted: Boolean) {
        if (granted) startIfNeeded() else stopIfRunning()
    }

    private fun startIfNeeded() {
        if (initialized.get()) {
            PostHog.optIn()
            return
        }
        val app = this.app ?: run {
            Timber.w("PostHog: setConsent(true) before init()")
            return
        }
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
        initialized.set(true)
    }

    private fun stopIfRunning() {
        if (initialized.get()) PostHog.optOut()
    }

    override fun track(event: String, props: Map<String, Any?>) {
        if (initialized.get()) PostHog.capture(event = event, properties = props.filterNonNull())
    }

    override fun screen(name: String, props: Map<String, Any?>) {
        if (initialized.get()) PostHog.screen(screenTitle = name, properties = props.filterNonNull())
    }

    override fun identifyOnSignIn(state: AuthState) {
        if (!initialized.get()) return
        if (state is AuthState.SignedIn) {
            // PII (email, name) intentionally NOT included.
            PostHog.identify(distinctId = state.session.userId)
        }
    }

    override fun reset() {
        if (initialized.get()) PostHog.reset()
    }

    private fun Map<String, Any?>.filterNonNull(): Map<String, Any> =
        mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
}
