# Cloud integration — Supabase + PostHog + FCM

Added 2026-05-05 as part of the cloud pivot (see `DECISIONS.md` → "Cloud pivot").

## What landed

| Layer          | Files                                                                                                                                                |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| Spec amendment | `CLAUDE.md`, `DECISIONS.md`                                                                                                                          |
| Build config   | `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `local.properties.example`                                                  |
| Supabase auth  | `data/auth/SupabaseClientProvider.kt`, `data/repository/AuthRepositoryImpl.kt`, `domain/repository/AuthRepository.kt`, `domain/model/AuthSession.kt` |
| Auth UI        | `ui/screen/auth/AuthScreen.kt`, `ui/screen/auth/AuthViewModel.kt`                                                                                    |
| DI             | `di/RepositoryModule.kt` (binding added), `di/AnalyticsModule.kt` (new)                                                                              |
| Analytics      | `data/analytics/AnalyticsTracker.kt`                                                                                                                 |
| Push           | `data/push/callNestMessagingService.kt`, `data/push/PushTokenSync.kt`, manifest service entry                                                        |
| App init       | `callNestApp.kt` calls `analytics.init(this)` and `pushTokenSync.registerCurrentToken()`                                                             |

## What you must do before the first build

### 1. Fill in `local.properties`

Copy `local.properties.example`, then populate (Android Studio normally creates `local.properties` automatically with `sdk.dir`; just append these):

```
SUPABASE_URL=https://YOUR_REF.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...
POSTHOG_API_KEY=phc_...
POSTHOG_HOST=https://us.i.posthog.com   # or eu.i.posthog.com
GOOGLE_OAUTH_WEB_CLIENT_ID=...           # only when activating Google sign-in
```

The build will succeed with empty values — the app will simply skip analytics init and Supabase will throw a clear runtime error on first call.

### 2. Drop in `google-services.json`

Firebase Console → Project settings → _Your apps_ → Add Android app with package
`com.callNest.app` (and `.debug` variant if you want push in debug). Download
`google-services.json` and place it at `app/google-services.json`. Gradle will
fail without it because the `google-services` plugin is now applied.

### 2b. Add the password-reset redirect URL to Supabase

Supabase → Authentication → URL Configuration → **Redirect URLs** → add:

```
callNest://auth/recovery
```

Without this, password-reset emails will land on a generic Supabase web page that can't return users to the app.

### 2c. Analytics consent (PostHog)

`PostHogTracker` no longer initializes automatically. The flag `SettingsDataStore.analyticsConsent` (default `false`) gates initialization. Toggle it via your settings UI when you ship the consent UI; until then, no events leave the device. Email/name are never sent — only the Supabase userId.

### 3. Create the `device_tokens` table in Supabase

See `app/src/main/assets/db/device_tokens.sql` for the canonical schema (run that in Supabase → SQL Editor).

### 4. Wire the auth gate into navigation

The screen exists at `ui/screen/auth/AuthScreen.kt`. To gate the app behind it:

- In `MainActivity` (or `callNestNavHost`), observe `AuthRepository.state` (or `AuthViewModel.authState`) at the top.
- If `AuthState.SignedOut`, render `AuthScreen { /* navigate into the app */ }`.
- If `AuthState.SignedIn`, render the existing nav graph.

I left this step to you so I don't accidentally break the existing onboarding/permission flow at `ui/navigation/callNestNavHost.kt`.

### 5. Activate Google sign-in (when you're ready)

1. Add `GOOGLE_OAUTH_WEB_CLIENT_ID` to `local.properties`.
2. In Supabase → Authentication → Providers → Google, paste the same Web Client ID + secret.
3. Uncomment the body of `AuthRepositoryImpl.signInWithGoogle`.
4. Uncomment the Google button in `AuthScreen.kt` and wire it to Credential Manager (`androidx.credentials:credentials` + `credentials-play-services-auth`) to obtain the ID token.

### 6. Run the build

You'll need to do this yourself — `CLAUDE.md` rule 1 forbids me from running gradle without explicit approval.

```
./gradlew assembleDebug
```

If first-build downloads time out, try `--no-daemon` and ensure `gradle wrapper --gradle-version 8.10.2` has been run.

## Privacy

The app now transmits user data. Update your privacy policy to disclose:

- Supabase (account email, hashed password) — auth provider
- PostHog (device events, screen views, user ID after sign-in) — product analytics
- FCM (device token) — push notifications

## Calling analytics from features

```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    fun onExportTapped() = analytics.track("export_started", mapOf("format" to "xlsx"))
}
```

## Calling auth from features

```kotlin
@HiltViewModel
class BarViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    val isSignedIn = auth.state.map { it is AuthState.SignedIn }
}
```
