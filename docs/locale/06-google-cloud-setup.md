# 06 — Google Cloud OAuth setup (for Drive backup)

callNest uses **AppAuth + Drive REST** for the optional Google Drive backup feature. No Google Play Services involved. This file walks you through getting the OAuth credentials Phase G needs.

Total time: ~10 minutes.

---

## Why this is needed

Phase G adds an optional "Save to Google Drive" toggle on the Backup screen. When the user enables it:

1. AppAuth opens a Chrome Custom Tab to Google's OAuth consent screen.
2. User picks their Google account and grants Drive scope.
3. We get an access token + refresh token.
4. We use the access token to call `https://www.googleapis.com/upload/drive/v3/files` and upload the `.cvb` blob.

For step 1 to work, Google needs to know your app exists. That's the **OAuth client** registration.

---

## Step 1 — Create a Google Cloud project

1. Open https://console.cloud.google.com.
2. Top bar → project dropdown → **New Project**.
3. Project name: `callNest` (or anything you like).
4. Location: leave as "No organization".
5. Click **Create**. Wait ~30 seconds.
6. Make sure the new project is selected in the top bar dropdown.

## Step 2 — Enable the Drive API

1. Sidebar → **APIs & Services** → **Library**.
2. Search "Google Drive API" → click it → **Enable**.
3. Wait for the green "API enabled" confirmation.

## Step 3 — Configure the OAuth consent screen

1. Sidebar → **APIs & Services** → **OAuth consent screen**.
2. User type: **External**. Click **Create**.
3. Fill in:
   - App name: `callNest`
   - User support email: your email
   - Developer contact email: your email
   - Logo, app domain, etc: skip (optional)
4. **Save and Continue**.
5. Scopes step — click **Add or Remove Scopes**, search and tick:
   - `.../auth/drive.file` (recommended — only files callNest creates)
   - OR `.../auth/drive` (full Drive access — only if you actually need to read existing files; we do not).
6. **Update** → **Save and Continue**.
7. Test users — add your own Gmail address as a test user so you can sign in while the app is in "testing" mode.
8. **Save and Continue** → **Back to dashboard**.

## Step 4 — Create the OAuth client

1. Sidebar → **APIs & Services** → **Credentials**.
2. **+ Create Credentials** → **OAuth client ID**.
3. Application type: **Web application** (yes, Web — not Android. AppAuth uses a custom URI scheme redirect, not the Android-client SHA-1 mechanism).
4. Name: `callNest Android (AppAuth)`.
5. Authorized redirect URIs — click **+ Add URI** and paste:
   ```
   com.callNest.app.debug:/oauth2redirect
   ```
   And another for the release build:
   ```
   com.callNest.app:/oauth2redirect
   ```
6. **Create**.
7. Copy the **Client ID** (looks like `1234567890-abcdefg.apps.googleusercontent.com`).
8. Copy the **Client secret** (looks like `GOCSPX-XXXXXXXXX`).

> A Web client _does_ issue a client secret. AppAuth uses it together with PKCE — the secret isn't a security boundary in this flow (AppAuth treats public clients correctly), but Google requires it on Web client types.

## Step 5 — Paste credentials into the app

Create `app/src/main/res/values/secrets.xml` (gitignored — we'll add it to `.gitignore` in Phase G):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="cv_drive_oauth_client_id">PASTE_CLIENT_ID_HERE</string>
    <string name="cv_drive_oauth_client_secret">PASTE_CLIENT_SECRET_HERE</string>
</resources>
```

Replace the two placeholder values. Save.

## Step 6 — Verify

After Phase G lands and you `./gradlew installDebug`:

1. Open the app → Settings → Backup.
2. Toggle "Save to Google Drive" on.
3. A Chrome Custom Tab should open at Google's sign-in page.
4. Pick your test-user account → grant `drive.file` scope.
5. Tab closes; the toggle row should now show "Signed in as `<your email>`".
6. Tap **Upload now**.
7. Open https://drive.google.com — a folder named **callNest Backups** should contain a `.cvb` file.

---

## Common errors

| Symptom                   | Cause                                                                 | Fix                                                                                                          |
| ------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `redirect_uri_mismatch`   | The authorized URI in step 4.5 doesn't match the scheme the app uses. | Verify both URIs are exact: `com.callNest.app.debug:/oauth2redirect` AND `com.callNest.app:/oauth2redirect`. |
| `access_denied`           | Your test-user isn't on the consent-screen Test Users list.           | Step 3.7 — add the email.                                                                                    |
| `invalid_client`          | Client ID or secret typo in `secrets.xml`.                            | Re-copy from the Credentials page.                                                                           |
| Token request returns 401 | App is in production OAuth state without verified domain.             | Keep the consent screen in "Testing" while developing.                                                       |

## When you're ready to ship publicly

Currently the OAuth consent screen is in **Testing** mode — only Test Users can sign in. To open it to anyone:

1. Console → **OAuth consent screen** → **Publish app**.
2. Google may require app verification (a few days, with a privacy policy URL and a homepage). For sideloaded distribution, this is rarely worth doing — keep it in Testing and just add each user manually as a Test User. Limit: 100 test users.

If you want unlimited users without verification, switch the OAuth client to **Internal** — but that requires a Google Workspace account.

---

## Don't commit secrets

Add to `.gitignore`:

```
app/src/main/res/values/secrets.xml
```

Phase G will add this for you.
