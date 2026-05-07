# Environment setup — where to get every key

This guide walks through obtaining every value referenced in `local.properties.example`.
Total time: ~20–30 minutes if you're starting from zero accounts.

After collecting the keys, paste them into `local.properties` (in the project root, next to `build.gradle.kts`). That file is auto-gitignored by Android tooling.

---

## Final `local.properties` shape

```properties
# Standard Android SDK location — Android Studio fills this automatically.
sdk.dir=/home/youruser/Android/Sdk

# Supabase
SUPABASE_URL=https://abcdxyz.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOi...long.jwt...

# PostHog
POSTHOG_API_KEY=phc_AbCdEf123...
POSTHOG_HOST=https://us.i.posthog.com

# Google OAuth (leave blank for now — Google sign-in is commented out)
GOOGLE_OAUTH_WEB_CLIENT_ID=
```

---

## 1. Supabase — `SUPABASE_URL` and `SUPABASE_ANON_KEY`

### Create a project

1. Go to https://supabase.com → **Sign in** (GitHub login is fastest).
2. Click **New project**.
3. Fill in:
   - **Name**: `callNest` (anything you like)
   - **Database password**: generate and store in a password manager — you don't need it for the app, but you'll want it later for SQL access.
   - **Region**: pick the one closest to your users (for India: `Mumbai (ap-south-1)`).
   - **Plan**: Free.
4. Click **Create new project**. Wait ~2 minutes for provisioning.

### Copy the keys

1. In the project dashboard left sidebar → click the **gear icon** (Project settings) at the bottom.
2. Click **API** (or **Data API** on newer dashboards).
3. You'll see:
   - **Project URL** → copy this into `SUPABASE_URL`.
     Example: `https://abcdxyzqwerty.supabase.co`
   - **Project API keys → `anon` `public`** → copy this into `SUPABASE_ANON_KEY`.
     It's a long JWT starting with `eyJhbGciOi...`. **Use the `anon` key, NOT the `service_role` key.** The service_role key is a secret and must never go in a mobile app.

### Enable email auth

1. Left sidebar → **Authentication** → **Providers**.
2. **Email** is enabled by default. If you want to skip email confirmation during dev, click **Email** → toggle off **Confirm email** → Save. (Re-enable for production.)

### Run the device_tokens SQL

1. Left sidebar → **SQL Editor** → **New query**.
2. Paste this and click **Run**:

   ```sql
   create table public.device_tokens (
     id uuid primary key default gen_random_uuid(),
     user_id uuid references auth.users on delete cascade,
     fcm_token text not null unique,
     updated_at timestamptz default now()
   );

   alter table public.device_tokens enable row level security;

   create policy "users manage own tokens" on public.device_tokens
     for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
   ```

You're done with Supabase.

---

## 2. PostHog — `POSTHOG_API_KEY` and `POSTHOG_HOST`

### Create a project

1. Go to https://posthog.com → **Get started — free**.
2. Sign up (Google login is fastest).
3. **IMPORTANT — choose region:**
   - **US Cloud** → host is `https://us.i.posthog.com`
   - **EU Cloud** → host is `https://eu.i.posthog.com`
     For India users, US is fine; EU if you have GDPR concerns. **You can't change this later** without creating a new project, so pick deliberately.
4. Project name: `callNest`. Skip the onboarding wizard (or click through it — doesn't matter).

### Copy the keys

1. Bottom-left → click your profile/project name → **Project settings**.
2. Section **Project variables**:
   - **Project API Key** → copy into `POSTHOG_API_KEY`. Starts with `phc_`.
3. `POSTHOG_HOST` is whichever region you picked above:
   - US: `https://us.i.posthog.com`
   - EU: `https://eu.i.posthog.com`

You're done with PostHog. Events will start appearing in **Activity → Live events** as soon as the app runs.

---

## 3. Firebase / FCM — `google-services.json` (no env key)

FCM doesn't use a `local.properties` value; it uses a JSON config file dropped into `app/`.

### Create a Firebase project

1. Go to https://console.firebase.google.com → **Add project**.
2. Name: `callNest`. Continue.
3. **Disable Google Analytics** when prompted (you already have PostHog) — uncheck it. Continue.
4. Wait for provisioning.

### Register the Android app

1. In the project → click the **Android icon** ("Add app" → Android).
2. Fill in:
   - **Android package name**: `com.callNest.app`
   - **App nickname**: `callNest` (optional)
   - **Debug signing certificate SHA-1**: leave blank for now. (Required for Google sign-in later — when you activate it, run `./gradlew signingReport` and paste the debug SHA-1 here.)
3. Click **Register app**.

### Optional: also register the debug variant

The app uses `applicationIdSuffix = ".debug"` for debug builds, which means debug builds have package id `com.callNest.app.debug`. To get push working in debug builds too:

1. In the same Firebase project → **Add app → Android** again.
2. Package name: `com.callNest.app.debug`
3. Same nickname + blank SHA-1.
4. Click **Register app**.

### Download `google-services.json`

1. After registration, Firebase prompts you to download `google-services.json`.
2. Save it to: `<project-root>/app/google-services.json`
   (so the path is `4. callNest/app/google-services.json`)
3. The single file works for both production and `.debug` variants — Firebase merges both registrations into one JSON.
4. **Do not commit this file** — add `app/google-services.json` to `.gitignore` if it isn't already.

The app's gradle `google-services` plugin will read this file on next build.

---

## 4. Google OAuth — `GOOGLE_OAUTH_WEB_CLIENT_ID` (skip for now)

Leave blank in `local.properties` until you're ready to activate Google sign-in. When you are:

1. Go to https://console.cloud.google.com.
2. Select your Firebase project (Firebase auto-creates a GCP project under the hood).
3. Top-left → **APIs & Services** → **Credentials**.
4. Click **Create Credentials → OAuth client ID**.
5. **Application type**: **Web application** (not Android — we use the Web client ID with Credential Manager).
6. Name: `callNest Web Client`. **Create**.
7. Copy the **Client ID** (ends with `.apps.googleusercontent.com`) → paste into `GOOGLE_OAUTH_WEB_CLIENT_ID`.
8. Open Supabase → **Authentication → Providers → Google** → enable, paste the same Web Client ID + the Client Secret from step 6 → Save.
9. Then uncomment the Google sign-in body in `data/repository/AuthRepositoryImpl.kt` and the Google button in `ui/screen/auth/AuthScreen.kt`.

---

## 5. Verify everything is connected

After filling `local.properties` and dropping `google-services.json`:

1. Build & install the debug app.
2. Open the auth screen, sign up with a test email.
3. **Supabase** → Authentication → Users → confirm the user appears.
4. **PostHog** → Activity → Live → confirm `auth_success` event appears.
5. **Firebase** → Cloud Messaging → check token registration in Logcat (`FCM token refreshed`); the row appears in `device_tokens` once a user is signed in.

If any of those don't work, the app logs everything via Timber — filter Logcat by tag `Timber` or by your package name.

---

## Quick reference table

| Key                          | Where to find it                                                     | Looks like                                               |
| ---------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------- |
| `SUPABASE_URL`               | Supabase → Project settings → API → Project URL                      | `https://abcd...supabase.co`                             |
| `SUPABASE_ANON_KEY`          | Supabase → Project settings → API → `anon` `public`                  | `eyJhbGciOi...` (long JWT)                               |
| `POSTHOG_API_KEY`            | PostHog → Project settings → Project API Key                         | `phc_AbCdEf123...`                                       |
| `POSTHOG_HOST`               | Region you chose at PostHog signup                                   | `https://us.i.posthog.com` or `https://eu.i.posthog.com` |
| `GOOGLE_OAUTH_WEB_CLIENT_ID` | Google Cloud → APIs & Services → Credentials → OAuth client (Web)    | `1234-abc.apps.googleusercontent.com`                    |
| `google-services.json`       | Firebase Console → Project settings → Your apps → Android → Download | binary JSON file → `app/google-services.json`            |
