# callNest — Technical Roadmap (Phase 1 / 2 / 3)

## Phase 1 — Launch (Offline-First Core)

### Platform & Build

- Native Android only
- Kotlin 2.0+ (K2 compiler)
- Jetpack Compose UI
- Gradle 8.10+ with version catalogs
- KSP for annotation processing
- Min SDK 26, Target SDK 35
- Multi-module structure (`:app`, `:core:*`, `:feature:*`)
- App Bundle (.aab) distribution
- R8 full mode for release
- Play App Signing

### UI

- Jetpack Compose
- Material 3 (accessibility primitives only)
- Custom Neo\* component layer (neumorphism)
- Navigation Compose 2.8+ (type-safe routes)
- Coil 3.x for images
- compose-shimmer for loading states
- lottie-compose for onboarding animations
- Markwon for note markdown rendering
- Light theme only

### Architecture

- Clean architecture lite (data/domain/ui)
- MVVM with Compose
- Single Activity
- Repository pattern
- UseCase classes for complex logic
- StateFlow + SharedFlow for reactivity
- Hilt for DI

### Data

- Room with FTS4 virtual tables
- DataStore Proto for settings
- EncryptedSharedPreferences (AndroidX Security Crypto / Tink) for secrets
- `userId: String` column (default `"local"`) on every entity — multi-tenant ready

### Background & System

- Kotlin Coroutines + Flow
- WorkManager for periodic jobs (sync, daily backup, follow-ups, update check)
- BroadcastReceiver for `PHONE_STATE`
- Foreground service for floating bubble
- AlarmManager `setExactAndAllowWhileIdle()` for follow-up reminders under Doze
- XML View for floating bubble overlay (Compose for everything else)

### Permissions

- Centralized `PermissionManager` class
- Handles: `READ_CALL_LOG`, `READ_CONTACTS`, `WRITE_CONTACTS`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `SCHEDULE_EXACT_ALARM`, `IGNORE_BATTERY_OPTIMIZATIONS`
- Centralized rationale + denied screens
- OEM-specific autostart deeplinks (Xiaomi, Oppo, Vivo, Samsung, Realme, OnePlus)

### Networking

- Ktor Client (for update manifest poll)
- kotlinx.serialization
- kotlinx-datetime

### Crypto (Backups)

- PBKDF2-HMAC-SHA256, 120k iterations
- AES-256-GCM
- `javax.crypto` standard library only
- Encrypted `.cvb` backup format

### Analytics & Crash Reporting

- Sentry Android SDK (crashes + ANR + basic perf)
- PostHog Android SDK (events + feature flags + session replay)
- Consent screen on first launch (DPDP Act 2023 compliance)
- Separate toggles for "Crash reports" and "Product analytics"
- Locked event taxonomy from day 1

### Billing (Scaffold Only, Inactive)

- Google Play Billing v7 dependency
- RevenueCat wrapper class
- Feature-flagged off

### Static Analysis & Quality

- Detekt
- Spotless + ktlint
- LeakCanary (debug builds)

### Testing

- JUnit 5
- Kotest assertions
- MockK
- Turbine (Flow testing)
- Room DAO instrumentation tests (in-memory DB)
- Maestro for E2E
- Paparazzi for Compose snapshot tests
- Kover for coverage
- Coverage targets: ≥80% on data/domain

### CI/CD

- GitHub Actions
- Gradle Build Cache + Configuration Cache
- Pipeline: build → unit tests → instrumented tests → R8 release → artifact upload
- Pre-commit hooks (Spotless, Detekt)

### Distribution

- Google Play Store (primary)
- CALL_LOG Permissions Declaration Form submission
- Internal testing → Closed testing (14+ days, 12+ testers) → Production
- Direct APK download from own website (fallback)
- Self-update via `versions.json` manifest poll + DownloadManager + SHA-256 verification
- FileProvider + ACTION_VIEW for system installer handoff

### Backup

- Local encrypted `.cvb` files in `Downloads/callNest/`
- DailyBackupWorker (24h periodicity)
- 7-day retention default
- Manual restore from local file picker

---

## Phase 2 — Monetization & Cloud (After Phase 1 Validates)

### Auth

- Clerk or Supabase Auth (managed)
- Email + phone OTP
- Google Sign-In (since Play Store version uses GMS anyway by Phase 2)
- Refresh token rotation
- Multi-device session management

### Backend (New Stack)

- NestJS + TypeScript
- PostgreSQL + Drizzle ORM
- Multi-tenant schema (`agency_id` + `user_id` on every row)
- Cloudflare R2 for encrypted blob storage
- BullMQ + Redis for job queue
- Hosted on Railway or Fly.io
- Postgres on Neon or Supabase

### Cloud Sync

- Per-user Postgres schema
- Local SQLite remains source of truth
- Cloud is a mirror (delta sync via timestamps)
- Conflict resolution: last-write-wins per row, with audit log
- Zero-knowledge architecture: server stores ciphertext blobs; user passphrase derives encryption key client-side
- Optional Google Drive backup upload (encrypted blob only)

### Premium Tier (Activate Phase 1 Scaffold)

- RevenueCat live integration
- Free tier limits: 100 calls/day, 5 tags, 7-day retention
- Premium: unlimited capture, unlimited tags, 90-day retention, multi-device sync, Drive backup, AI features
- Subscription pricing: ₹199/month or ₹1,499/year
- Webhook-driven entitlement sync (RevenueCat → backend)

### AI Features (Server-Side)

- Claude API or OpenAI for inference
- Auto-tag suggestions based on call patterns
- Note summarization
- Weekly insight digests
- Natural-language follow-up reminders
- Inference queue via BullMQ
- No call audio (recording remains out of scope)
- All AI requests opt-in with explicit consent
- PII redaction before LLM call

### Voice Notes

- `expo-speech-recognition` or native Android `SpeechRecognizer`
- Server-side Whisper API for premium transcription
- Encrypted at rest in Room (BLOB column)

### Lead Pipeline Stages

- Ordered states: New → Contacted → Qualified → Won / Lost
- Pairs with existing flat tag system
- Stage transition history table

### Smart Notifications

- Daily morning briefing (9 AM): hot leads count, follow-ups today, unsaved inquiries
- Smart follow-up suggestions in post-call popup
- Server-driven via FCM (now acceptable since Play Store + GMS)

### WhatsApp & SMS Integration

- WhatsApp deeplink button (`https://wa.me/91XXXXX`)
- SMS quick-reply templates
- Both are intent launches — no message reading

### Number Reputation Lookup

- Hash phone number client-side (SHA-256)
- Check against server spam database
- Privacy-preserving Truecaller alternative
- Crowdsourced from user reports

### Export Enhancements

- Export to Google Sheets (OAuth + Sheets API)
- Scheduled exports (weekly/monthly auto-email to user)
- Custom column mapping

### Editor Migrations

- NoteEditor: AlertDialog → bottom sheet
- TagEditor: AlertDialog → bottom sheet
- FollowUpEditor: bottom sheet from day 1

### Backup Enhancements

- Backup integrity verification (daily decrypt-test)
- Restore preview before commit
- Cross-device restore via cloud
- Multi-version backup chains

### Dark Mode

- Full neumorphism dark theme
- Updated Appendix E tokens
- System-follow + manual toggle

### Observability

- Sentry server SDK (NestJS)
- Better Stack or Grafana Cloud (logs + uptime)
- PostHog server-side events for revenue/conversion tracking

### Agency White-Label Foundation

- Agency dashboard (Next.js 15 + shadcn/ui)
- Multi-tenant runtime branding (Mode A): single APK fetches branding config by agency code
- Agency signup, billing, end-user list, aggregate stats only (no plaintext user data)
- Agency tiers: Starter / Growth / Enterprise
- Branding controls: logo, colors, app name, custom support contact
- White-label config served via signed JWT to client app

### CMS for Help Docs

- Sanity, Strapi, or Payload CMS
- Replaces bundled markdown `AssetDocsLoader`
- Per-agency content overrides
- LRU cache for offline reading

### Database Encryption at Rest

- SQLCipher integration (~5MB APK cost)
- For privacy-conscious agencies / enterprise tier

---

## Phase 3 — Scale & Expansion (Future)

### Per-Agency White-Label APKs (Mode B)

- CI/CD pipeline producing signed APKs per agency config JSON
- Each agency owns its own Play Console listing
- Agency-managed signing keys (vault-stored)
- Automated release pipeline per agency

### Enterprise Tier

- SSO (SAML, OIDC)
- Custom domain for agency dashboard
- SLA + dedicated support
- Audit log export
- On-premise / VPC deployment option
- Custom AI model fine-tuning per agency

### iOS Companion App

- Compose Multiplatform OR native Swift/SwiftUI
- iOS does not expose call log (Apple platform restriction) — read-only sync from Android primary device only
- Notes, follow-ups, search, exports all available
- Apple Sign-In + Sign-In with Apple required by App Store

### Desktop App

- Compose Multiplatform Desktop OR Tauri (web-based)
- For accountants and agency operators
- Read-only by default, edit mode behind permission
- Bulk actions: tag 100 calls at once, export wizards, advanced filters

### Public API

- REST + GraphQL endpoints
- API keys per agency
- Rate-limited tiers
- Zapier / Make.com integrations
- Webhook support for inbound CRM events
- OpenAPI 3.1 spec + auto-generated SDKs

### Advanced AI

- On-device LLM (Gemini Nano or Llama 3.2 1B) for privacy-sensitive inference
- Real-time call sentiment cues (post-call only, from notes/duration patterns — never audio)
- Predictive follow-up timing ("This lead converts best if called Tuesday 11 AM")
- Natural language queries ("Show me all hot leads who called twice last week")
- AI agent that auto-drafts follow-up SMS templates per lead profile

### Team Mode

- Multi-user per business
- Roles: Owner, Manager, Sales Rep
- Lead assignment + reassignment
- Team dashboard with per-rep stats
- Shared tags + private tags
- Activity feed
- Real-time presence (Socket.io or Pusher)

### Advanced Analytics

- Conversion funnels per agency
- Cohort retention reports
- Churn prediction
- Revenue attribution per lead source

### Marketplace

- Third-party tag rule packs (e.g., "Real Estate Starter Kit", "Wedding Photographer Templates")
- Revenue share with creators
- In-app purchase for premium rule packs

### Compliance & Security

- SOC 2 Type II certification
- ISO 27001
- HIPAA mode for medical clinics (regulated tier)
- DPDP Act Significant Data Fiduciary registration
- Penetration testing + bug bounty program

### Localization

- Multi-language UI (Hindi, Tamil, Telugu, Marathi, Bengali, Gujarati first)
- Right-to-left support (Urdu, Arabic — for Middle East expansion)
- Region-specific number formatting and call log parsing

### Geographic Expansion

- Southeast Asia (Indonesia, Vietnam, Philippines — similar SMB profile)
- Middle East (UAE, Saudi)
- Africa (Nigeria, Kenya — high small-business call volume)

### Alternative Distribution

- Samsung Galaxy Store
- Xiaomi GetApps / MIUI Store
- Huawei AppGallery (GMS-free build)
- Amazon Appstore
- Aptoide (agency-resale friendly)

### Hardware Integrations

- Bluetooth headset call quality scoring
- Smartphone PBX integration (for businesses with multiple SIMs)
- Call center hardware (Plivo, Exotel) for outbound campaign tracking

---

When you want, I can convert any phase into a week-by-week build sprint plan or a `.claude/` task breakdown for autonomous execution.
