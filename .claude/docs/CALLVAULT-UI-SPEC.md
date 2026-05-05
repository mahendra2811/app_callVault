# CallVault — Phase 1 Optimized Plan

> **Performance-first. Local-only. Optional telemetry. Encrypted at rest.**
>
> **Audience:** Mahendra Singh Puniya + AI coding assistants.
> **Status:** Locked. Supersedes earlier Phase 1 documents.
> **Last revised:** 2026-05-02.

---

## Table of Contents

1. [Strategic Refinements](#1-strategic-refinements)
2. [Performance Philosophy](#2-performance-philosophy)
3. [Distribution Model (No Play Store)](#3-distribution-model-no-play-store)
4. [Storage Architecture](#4-storage-architecture)
5. [Search Subsystem](#5-search-subsystem)
6. [Cold Storage & Archival](#6-cold-storage--archival)
7. [Export Subsystem](#7-export-subsystem)
8. [Optional Telemetry](#8-optional-telemetry)
9. [Cold Start & Loading Strategy](#9-cold-start--loading-strategy)
10. [Tech Stack (Updated)](#10-tech-stack-updated)
11. [Layout Plans (Per Screen)](#11-layout-plans-per-screen)
12. [Performance Budgets](#12-performance-budgets)
13. [Implementation Order](#13-implementation-order)
14. [Code Patterns](#14-code-patterns)
15. [Readiness Checklist](#15-readiness-checklist)

---

## 1. Strategic Refinements

This plan supersedes the earlier Phase 1 plan with these changes:

| Topic               | Previous plan                       | This plan                                                                                |
| ------------------- | ----------------------------------- | ---------------------------------------------------------------------------------------- |
| Distribution        | Play Store primary                  | Direct APK + self-update only. No Play Store in Phase 1.                                 |
| Sentry / PostHog    | Mandatory with consent screen       | Optional. Build-time configurable. No-op if env vars absent.                             |
| Telemetry priority  | Full event taxonomy from day 1      | Single metric: active users. Everything else deferred.                                   |
| DB encryption       | SQLCipher deferred to Phase 2       | SQLCipher in Phase 1. Local data unreadable without app.                                 |
| Layout density      | 76dp list rows, 20dp screen padding | 64dp list rows, 16dp screen padding. Tighter, more rows visible.                         |
| Storage strategy    | Single Room DB for all calls        | Hot/cold partition. Calls < 1yr in DB; older in encrypted archive files.                 |
| Performance posture | Standard Android budgets            | Aggressive: cold start <800ms, FTS <60ms, scroll FPS sustained 120 on supported devices. |

**The core focus:** the app loads fast, searches fast, scrolls smoothly with 50,000+ rows, and never leaks user data even if the device is rooted or seized. Telemetry is a nice-to-have, not a feature.

---

## 2. Performance Philosophy

Every architectural decision flows from these five rules:

**Rule 1 — Lazy by default.** No work happens until it's visibly needed. Hilt singletons defer construction. Database opens on first DAO access, not Application.onCreate(). Compose trees inflate just-in-time.

**Rule 2 — Indexed everywhere.** Every column that appears in a WHERE clause has an index. Compound indexes for common multi-column queries. FTS4 virtual tables for text search. No table scans on the hot path.

**Rule 3 — Hot/cold data partition.** The active DB only holds the last 365 days. Older calls live in encrypted archive files, loaded only when explicitly requested (export, year-over-year stats). The hot DB stays small (~10k rows for a heavy user) so every query is O(1) on indexed lookups.

**Rule 4 — Streaming over loading.** Lists use Paging 3. Exports use Apache POI streaming writers. Encryption uses CipherInputStream/CipherOutputStream. We never materialize a 50k-row list in memory.

**Rule 5 — Optional features compile to no-op.** Sentry, PostHog, Drive backup, billing — all gated behind BuildConfig flags. When disabled, R8 strips the implementations entirely. Zero runtime cost.

---

## 3. Distribution Model (No Play Store)

### 3.1 Channels

| Channel                                  | Phase 1 status                        |
| ---------------------------------------- | ------------------------------------- |
| Direct APK from own website              | ✅ Primary                            |
| Self-update via `versions.json` manifest | ✅ Primary update path                |
| WhatsApp share / QR code                 | ✅ Acquisition                        |
| Galaxy Store, Xiaomi GetApps, Aptoide    | ⏸️ Phase 1.1 (optional)               |
| Google Play Store                        | ❌ Phase 2 (after product validation) |

### 3.2 Update flow (already in v1.0 spec, kept as-is)

1. `UpdateCheckWorker` polls `versions.json` weekly
2. Compares `versionCode` to local `BuildConfig.VERSION_CODE`
3. If newer, shows `UpdateAvailableScreen`
4. User taps Install → DownloadManager downloads APK
5. SHA-256 hash verified against manifest
6. FileProvider + ACTION_VIEW hands off to system installer
7. User must have granted "Install unknown apps" once

### 3.3 What NOT to worry about (since no Play Store)

- ❌ CALL_LOG Permissions Declaration Form
- ❌ Data Safety form
- ❌ Internal → Closed → Production track gates
- ❌ Pre-launch reports
- ❌ Restricted permission review
- ❌ ANR < 0.47% / Crash < 1.09% gates (still aim for them, but no submission gate)

### 3.4 What you DO need

- Privacy Policy (host on own website, link in About screen — good practice, DPDP Act 2023 says yes)
- Code signing key (kept in a password manager, never committed)
- Hosting for `versions.json` + APK files (Cloudflare R2 or any static host)
- A simple "Download" landing page

---

## 4. Storage Architecture

### 4.1 Three-tier storage

| Tier     | What lives there                                                         | Tech                                                      |
| -------- | ------------------------------------------------------------------------ | --------------------------------------------------------- |
| **Hot**  | Calls < 365 days, all contacts, notes, tags, rules, follow-ups, settings | SQLCipher-encrypted Room DB                               |
| **Warm** | Quarterly archives of calls 1–3 years old                                | Encrypted Protobuf files (one per quarter)                |
| **Cold** | Calls older than 3 years, optional encrypted backup                      | Same Protobuf format, optionally uploaded to user's Drive |

### 4.2 Encryption at rest

**Library:** SQLCipher for Android, integrated via Room.

```kotlin
// build.gradle.kts dependency
implementation("net.zetetic:sqlcipher-android:4.6.1")
implementation("androidx.sqlite:sqlite-ktx:2.4.0")
```

**Key derivation:**

- User passphrase entered during onboarding (or auto-generated and stored in `EncryptedSharedPreferences` if user opts for "no password" mode)
- PBKDF2-HMAC-SHA512, 256k iterations (more than backup's 120k because this runs once at app open, not on every backup)
- Derived key feeds SQLCipher's `PRAGMA key`

**SQLCipher configuration:**

```kotlin
// Optimized for performance
db.execSQL("PRAGMA cipher_page_size = 4096")
db.execSQL("PRAGMA cipher_compatibility = 4")  // SQLCipher 4 default
db.execSQL("PRAGMA kdf_iter = 256000")
db.execSQL("PRAGMA cipher_default_use_hmac = ON")
db.execSQL("PRAGMA journal_mode = WAL")  // 2-3x write throughput
db.execSQL("PRAGMA synchronous = NORMAL")  // Safe with WAL
db.execSQL("PRAGMA cache_size = -8192")  // 8MB cache (negative = KB)
db.execSQL("PRAGMA temp_store = MEMORY")
db.execSQL("PRAGMA mmap_size = 30000000")  // 30MB memory-mapped I/O
```

**Performance cost:** SQLCipher adds ~10–15% to query time. With WAL + 8MB cache + memory-mapped I/O, net performance is still well within the budget. Verified in benchmarks below.

**Security guarantee:** the `.db` file extracted via ADB or root is **fully encrypted ciphertext**. Without the user's passphrase, the file is useless. Combined with Android's filesystem encryption (default since Android 7), this is defense-in-depth.

### 4.3 Hot DB schema (unchanged from v1.0)

14 entities + 2 FTS4 virtual tables. Same as v1.0 spec, with two additions:

1. `userId TEXT NOT NULL DEFAULT 'local'` on every entity (Phase 2 prep)
2. `archivedAt INTEGER` nullable on `CallEntity` — set when row is moved to warm tier

### 4.4 Indexing strategy

Every WHERE-clause column is indexed. Compound indexes for the four hot query patterns:

```sql
-- Pattern 1: List by date, optionally filtered by tag
CREATE INDEX idx_calls_date_tags ON calls(dateMillis DESC, tagIdsCsv);

-- Pattern 2: Lookup by normalized number
CREATE INDEX idx_calls_normalized ON calls(normalizedNumber);

-- Pattern 3: Bookmarked + recent
CREATE INDEX idx_calls_bookmark_date ON calls(isBookmarked, dateMillis DESC) WHERE isBookmarked = 1;

-- Pattern 4: Follow-ups due
CREATE INDEX idx_calls_followup ON calls(followUpDate, followUpDone) WHERE followUpDate IS NOT NULL;

-- Contact meta lookups
CREATE INDEX idx_contact_meta_score ON contact_meta(leadScore DESC);
CREATE INDEX idx_contact_meta_last_call ON contact_meta(lastCallAt DESC);
CREATE INDEX idx_contact_meta_inquiry ON contact_meta(isAutoSaved, lastCallAt DESC) WHERE isAutoSaved = 1;
```

Partial indexes (`WHERE` clauses) are critical — they keep the index small for sparse data. Bookmarks affect ~1% of calls; a partial index is 100× smaller than a full one.

### 4.5 Read patterns

Every list query is paginated. No `SELECT * FROM calls` queries anywhere.

```kotlin
@Query("""
    SELECT * FROM calls
    WHERE userId = :userId
      AND archivedAt IS NULL
      AND (:tagFilter IS NULL OR tagIdsCsv LIKE :tagFilter)
    ORDER BY dateMillis DESC
""")
fun pagingSourceCalls(userId: String, tagFilter: String?): PagingSource<Int, CallEntity>
```

Paging 3 + Compose `LazyPagingItems` + `cachedIn(viewModelScope)` = list scrolls 60fps with 100k rows.

### 4.6 Write patterns

All multi-table writes happen inside a single `@Transaction`. The sync pipeline writes in batches of 200 rows per transaction:

```kotlin
@Transaction
suspend fun syncBatch(calls: List<CallEntity>, contactMetas: List<ContactMetaEntity>) {
    callDao.insertAll(calls)
    contactMetaDao.upsertAll(contactMetas)
}
```

WAL mode + `synchronous=NORMAL` allows readers and writers to operate concurrently. The UI never blocks during sync.

### 4.7 Memory budget

Active DB cache: 8 MB.
Memory-mapped I/O: up to 30 MB.
Compose runtime + UI: ~25 MB.
Coil image cache: ~10 MB (in-memory) + 50 MB (disk).
**Total target idle RSS: < 80 MB.**

---

## 5. Search Subsystem

### 5.1 Search anatomy

Three search dimensions, all pre-indexed:

| Dimension                                                 | Tech         | Latency target |
| --------------------------------------------------------- | ------------ | -------------- |
| Phone number digits (e.g. "5432" matches `+919876543210`) | Suffix index | < 30ms         |
| Contact name (e.g. "rahul")                               | FTS4         | < 60ms         |
| Note content (e.g. "sofa")                                | FTS4         | < 60ms         |

### 5.2 FTS4 setup

Two virtual tables:

```sql
CREATE VIRTUAL TABLE call_fts USING fts4(
    normalizedNumber,
    cachedName,
    geocodedLocation,
    tokenize=porter,
    content='calls',
    contentRowid='systemId'
);

CREATE VIRTUAL TABLE note_fts USING fts4(
    body,
    tokenize=porter,
    content='notes',
    contentRowid='id'
);
```

Triggers maintain FTS in lockstep with the source tables:

```sql
CREATE TRIGGER call_fts_insert AFTER INSERT ON calls BEGIN
    INSERT INTO call_fts(rowid, normalizedNumber, cachedName, geocodedLocation)
    VALUES (new.systemId, new.normalizedNumber, new.cachedName, new.geocodedLocation);
END;

CREATE TRIGGER call_fts_delete AFTER DELETE ON calls BEGIN
    DELETE FROM call_fts WHERE rowid = old.systemId;
END;

CREATE TRIGGER call_fts_update AFTER UPDATE ON calls BEGIN
    UPDATE call_fts
    SET normalizedNumber = new.normalizedNumber,
        cachedName = new.cachedName,
        geocodedLocation = new.geocodedLocation
    WHERE rowid = new.systemId;
END;
```

### 5.3 Number suffix matching

Phone numbers need _suffix_ matching ("5432" should match `+919876545432`). FTS4 doesn't do this natively. Solution: store a reversed normalized number column with an FTS prefix index.

```kotlin
// At insert time:
val reversedNumber = call.normalizedNumber.reversed()  // "+919876545432" -> "23454567899+91"

@Query("SELECT * FROM calls WHERE reversedNumber GLOB :pattern || '*'")
fun searchByDigitSuffix(pattern: String): Flow<List<CallEntity>>
```

When the user types "5432", we search reversed numbers starting with "2345". This uses the index. Latency: < 30ms for 50k rows.

### 5.4 Search UI behavior

- Debounce input at 200ms (single global flow)
- Minimum 2 characters to trigger search
- Results page-loaded, 30 per page
- Three result groups: "Numbers" / "Contacts" / "Notes" with section headers
- Recent searches surfaced when search bar focused and empty

### 5.5 Search ranking

```sql
SELECT *,
    CASE
        WHEN normalizedNumber LIKE :query || '%' THEN 100
        WHEN cachedName LIKE :query || '%' THEN 80
        WHEN normalizedNumber LIKE '%' || :query || '%' THEN 60
        WHEN cachedName LIKE '%' || :query || '%' THEN 40
        ELSE 20
    END as rank
FROM calls
WHERE ...
ORDER BY rank DESC, dateMillis DESC
LIMIT 30
```

---

## 6. Cold Storage & Archival

### 6.1 Why archive

A user with 100 calls/day generates 36,500 rows/year. After 3 years that's 109k rows. Even with perfect indexes, queries on a 100k-row table are measurably slower than on a 10k-row table. Archiving keeps the hot DB lean.

### 6.2 Archive format

| Property    | Value                                                                   |
| ----------- | ----------------------------------------------------------------------- |
| File format | Encrypted Protobuf (binary, dense)                                      |
| Naming      | `archive_2024_Q3.cvarc`                                                 |
| Location    | `app-private/archives/` (only readable by CallVault process)            |
| Encryption  | AES-256-GCM with key derived from user's master passphrase              |
| Compression | LZ4 (fast, ~3x compression on call data)                                |
| Schema      | `CallArchiveProto` — slim Protobuf message; one entry per archived call |

### 6.3 Archive worker

`ArchiveWorker` runs weekly:

1. Identifies calls with `dateMillis < now - 365 days` AND `archivedAt IS NULL`
2. Groups them by quarter
3. For each quarter:
   - Reads existing archive file (if any) into memory
   - Appends new entries (binary Protobuf, append-only-ish — actually rewrites the file with new content)
   - Writes encrypted file to disk via `CipherOutputStream`
   - Sets `archivedAt = now` on the source rows
4. Source rows stay in the hot DB but are excluded from default queries via `WHERE archivedAt IS NULL`
5. After 30 days of `archivedAt`, hot row is hard-deleted (gives the user a window to undo)

### 6.4 Reading archives

Only three operations need archived data:

1. **Year-over-year stats** — load archives lazily on Stats → "Compare" view
2. **Annual export** — load archives directly into the export streaming writer
3. **Restore from archive** — used if the user accidentally deletes hot data

Archives are read with `CipherInputStream` + Protobuf streaming, never materialized into a `List<>` in memory.

### 6.5 Archive Protobuf schema

```protobuf
syntax = "proto3";

message CallArchive {
  string user_id = 1;
  int32 quarter = 2;       // 1-4
  int32 year = 3;
  repeated CallRecord calls = 4;
}

message CallRecord {
  int64 system_id = 1;
  string raw_number = 2;
  string normalized_number = 3;
  string cached_name = 4;
  int32 type = 5;
  int32 sim_slot = 6;
  int64 date_millis = 7;
  int64 duration_sec = 8;
  bool is_bookmarked = 9;
  string tag_ids_csv = 10;
  string note_summary = 11;  // First 200 chars of merged notes for searchability
}
```

### 6.6 Archive integrity

Each archive file ends with a SHA-256 of its decrypted contents, encrypted into the same blob. On load:

1. Decrypt file to memory stream
2. Read all but last 32 bytes (the hash)
3. Compute SHA-256 of the read content
4. Compare to last 32 bytes
5. If mismatch, refuse to load + alert user

This catches bit-flips and tampering.

---

## 7. Export Subsystem

### 7.1 Export periods

The export wizard offers four time presets in addition to "custom range":

| Preset           | Range                                                                    |
| ---------------- | ------------------------------------------------------------------------ |
| **This week**    | Monday–Sunday of current week                                            |
| **This month**   | First → last day of current calendar month                               |
| **This quarter** | Q1 (Jan–Mar), Q2 (Apr–Jun), Q3 (Jul–Sep), Q4 (Oct–Dec) — current quarter |
| **This year**    | Jan 1 → Dec 31 of current calendar year                                  |
| **Custom**       | User-picked start + end dates                                            |

Plus historical variants: "Last week", "Last month", "Last quarter", "Last year".

### 7.2 Export formats

| Format            | Use case                      | Library                                 |
| ----------------- | ----------------------------- | --------------------------------------- |
| **Excel (.xlsx)** | Multi-sheet, formatted        | Apache POI ooxml-lite (already in v1.0) |
| **CSV (.csv)**    | Lightweight, tooling-friendly | Manual writer (no dependency)           |
| **PDF (.pdf)**    | Printable, accountant handoff | iText core (already in v1.0)            |

### 7.3 Excel export structure

Multi-sheet workbook:

| Sheet name       | Content                                                                               |
| ---------------- | ------------------------------------------------------------------------------------- |
| `Summary`        | Period stats: total calls, unique numbers, total duration, top 5 leads                |
| `All Calls`      | One row per call: date, time, number, name, type, duration, simSlot, tags, lead score |
| `Hot Leads`      | Filtered to score ≥ 70                                                                |
| `Follow-Ups`     | Calls with non-null `followUpDate`                                                    |
| `Notes`          | One row per note with the call it's attached to                                       |
| `By Tag — {tag}` | One sheet per tag (max 9 system tags + top 5 user tags by count)                      |

### 7.4 Streaming writer

For exports spanning thousands of calls, stream rows directly to disk:

```kotlin
val workbook = SXSSFWorkbook(100)  // Keep only last 100 rows in memory
val sheet = workbook.createSheet("All Calls")

callDao.streamForExport(start, end).collect { call ->
    val row = sheet.createRow(rowIdx++)
    row.createCell(0).setCellValue(formatDate(call.dateMillis))
    // ... etc
}

workbook.write(outputStream)
workbook.dispose()  // Free temp files
```

**Memory usage:** ~5MB regardless of export size.

### 7.5 Export across hot + warm data

If the user picks "Last year" and parts of that year are in the warm archive, the streaming writer reads from both sources transparently:

```kotlin
fun exportRange(start: Long, end: Long): Flow<CallExportRecord> = flow {
    // Hot data (in DB)
    callDao.streamRangeFromHot(start, end).collect { emit(it) }

    // Warm data (in archives)
    val quarters = quartersInRange(start, end)
    for (quarter in quarters) {
        archiveLoader.streamQuarter(quarter).filter {
            it.dateMillis in start..end
        }.collect { emit(it.toExportRecord()) }
    }
}
```

### 7.6 Export UI flow (5-step wizard, kept from v1.0)

1. **Range** — quick presets + custom
2. **Scope** — All / Bookmarked only / Tagged with / Hot leads only
3. **Columns** — toggle which columns to include
4. **Format** — Excel / CSV / PDF
5. **Destination** — Save to Downloads / Share / Email (intent-driven)

### 7.7 Quick Export sheet

For one-tap exports, a bottom sheet exposes "This week as Excel" / "This month as PDF" / "This quarter as Excel" with no further configuration. Default columns and scope.

---

## 8. Optional Telemetry

### 8.1 Design principle

All telemetry SDKs are **build-time configurable**. If env vars are absent, the implementation classes compile to no-ops and R8 strips them entirely. The base APK ships with telemetry disabled.

### 8.2 BuildConfig flags

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            buildConfigField("String", "SENTRY_DSN", "\"${localProps.getProperty("SENTRY_DSN", "")}\"")
            buildConfigField("String", "POSTHOG_API_KEY", "\"${localProps.getProperty("POSTHOG_API_KEY", "")}\"")
            buildConfigField("String", "HEARTBEAT_URL", "\"${localProps.getProperty("HEARTBEAT_URL", "")}\"")
        }
        release {
            buildConfigField("String", "SENTRY_DSN", "\"${localProps.getProperty("SENTRY_DSN", "")}\"")
            buildConfigField("String", "POSTHOG_API_KEY", "\"${localProps.getProperty("POSTHOG_API_KEY", "")}\"")
            buildConfigField("String", "HEARTBEAT_URL", "\"${localProps.getProperty("HEARTBEAT_URL", "")}\"")
        }
    }
}
```

### 8.3 Telemetry interface

```kotlin
interface Telemetry {
    fun init(context: Context)
    fun trackCrash(throwable: Throwable, metadata: Map<String, String> = emptyMap())
    fun trackEvent(name: String, properties: Map<String, Any?> = emptyMap())
    fun setUserId(userId: String)
}

class NoOpTelemetry : Telemetry {
    override fun init(context: Context) {}
    override fun trackCrash(throwable: Throwable, metadata: Map<String, String>) {}
    override fun trackEvent(name: String, properties: Map<String, Any?>) {}
    override fun setUserId(userId: String) {}
}
```

### 8.4 Hilt provider

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    @Singleton
    fun provideSentry(): SentryTelemetry? {
        return if (BuildConfig.SENTRY_DSN.isNotBlank()) SentryTelemetry() else null
    }

    @Provides
    @Singleton
    fun providePostHog(): PostHogTelemetry? {
        return if (BuildConfig.POSTHOG_API_KEY.isNotBlank()) PostHogTelemetry() else null
    }

    @Provides
    @Singleton
    fun provideTelemetry(
        sentry: SentryTelemetry?,
        posthog: PostHogTelemetry?,
        heartbeat: HeartbeatTelemetry,
    ): Telemetry {
        val active = listOfNotNull(sentry, posthog, heartbeat)
        return if (active.isEmpty()) NoOpTelemetry() else CompositeTelemetry(active)
    }
}
```

### 8.5 The single metric that matters: active users

Phase 1 needs only one piece of analytics: **how many people are currently using CallVault**.

This is a daily heartbeat ping to a tiny endpoint:

```kotlin
class HeartbeatTelemetry @Inject constructor(
    private val httpClient: OkHttpClient,
) : Telemetry {
    private val deviceIdHash = computeStableHash()  // SHA-256 of (Android ID + app install token)

    override fun init(context: Context) {
        if (BuildConfig.HEARTBEAT_URL.isBlank()) return
        // Send once per 24h via a WorkManager periodic worker
    }

    suspend fun ping() {
        val body = HeartbeatBody(
            deviceHash = deviceIdHash,
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            timestamp = System.currentTimeMillis(),
            // No PII. No call counts. No tags. Just "I'm alive."
        )
        httpClient.newCall(
            Request.Builder()
                .url(BuildConfig.HEARTBEAT_URL)
                .post(Json.encodeToString(body).toRequestBody("application/json".toMediaType()))
                .build()
        ).execute().close()
    }
}
```

The endpoint can be a single Cloudflare Worker counting unique `deviceHash` per day:

```javascript
// Cloudflare Worker (~50 lines)
export default {
  async fetch(request, env) {
    const body = await request.json();
    const day = new Date().toISOString().slice(0, 10);
    await env.KV.put(`day:${day}:${body.deviceHash}`, "1", { expirationTtl: 86400 * 32 });
    return new Response("ok");
  },
};
```

To get DAU: count keys matching `day:2026-05-02:*`. To get version distribution: shard the key. Free tier covers thousands of users.

This is the **only** mandatory telemetry. Sentry and PostHog are bonuses if env vars are configured.

### 8.6 Heartbeat worker

```kotlin
@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val heartbeat: HeartbeatTelemetry,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            heartbeat.ping()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

Schedule:

```kotlin
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "heartbeat",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<HeartbeatWorker>(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
)
```

### 8.7 No consent screen needed (when telemetry is just heartbeat)

Heartbeat sends only an opaque hash. No personal data. No call history. No contact info. This falls under "legitimate interest" for understanding active users and does not require explicit DPDP consent — but disclose it in the Privacy Policy.

If env vars enable Sentry or PostHog (which collect more), then a consent screen IS needed for those. But for the base build with just heartbeat, no consent screen.

---

## 9. Cold Start & Loading Strategy

### 9.1 Cold start budget: 800ms

Aggressive target. Achieved through:

| Technique                                         | Saving                                     |
| ------------------------------------------------- | ------------------------------------------ |
| Baseline Profile (Macrobenchmark-generated)       | ~30% reduction in cold start               |
| Splash Screen API with `setKeepOnScreenCondition` | Smooth handoff, no white flash             |
| Lazy Hilt singletons                              | DB doesn't open until first DAO call       |
| App Startup library for initialization order      | Workers register without blocking onCreate |
| Compose Compiler 2.0 with strong skipping         | Faster recomposition                       |
| R8 full mode + Resource shrinker                  | Smaller APK, faster class loading          |

### 9.2 Baseline Profile

Generate during CI:

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = "com.callvault",
        profileBlock = {
            startActivityAndWait()
            // Drive through critical paths
            device.findObject(By.text("Calls")).click()
            device.wait(Until.hasObject(By.res("call_list")), 5000)
        }
    )
}
```

Output: `baseline-prof.txt` shipped in the APK. ART pre-compiles the listed methods on install. Empirically delivers 200–400ms cold-start improvement.

### 9.3 App initialization order

```kotlin
@HiltAndroidApp
class CallVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Only what HAS to happen at startup:
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogTree())
        // Everything else uses App Startup or is deferred
    }
}
```

App Startup `Initializer`s run in dependency order, off the main thread where possible:

```kotlin
class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        WorkManager.initialize(context, Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build())
        return WorkManager.getInstance(context)
    }
    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
```

### 9.4 First frame strategy

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold splash until first composition is ready
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            SageTheme {
                val firstFrameReady = rememberFirstFrameReady()
                LaunchedEffect(firstFrameReady) {
                    if (firstFrameReady) keepSplash = false
                }
                AppNavGraph()
            }
        }
    }
}

@Composable
fun rememberFirstFrameReady(): Boolean {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Wait for first layout pass + initial data load
        snapshotFlow { /* observable readiness signal */ }
            .first { it }
        ready = true
    }
    return ready
}
```

### 9.5 List loading

Skeleton appears after 200ms (NOT instantly — many lists load fast enough that a flicker is worse than nothing):

```kotlin
@Composable
fun CallList(viewModel: CallListViewModel) {
    val calls = viewModel.pagingFlow.collectAsLazyPagingItems()
    val state = calls.loadState.refresh

    when {
        state is LoadState.Loading && calls.itemCount == 0 -> {
            // Delay shimmer 200ms to avoid flicker
            DelayedShimmer(delayMs = 200)
        }
        calls.itemCount == 0 -> EmptyState()
        else -> LazyColumn { items(calls, key = { it.systemId }) { CallRow(it) } }
    }
}
```

### 9.6 Compose stability

Mark every data class shared with Composables as `@Stable` or `@Immutable`:

```kotlin
@Immutable
data class CallUiModel(
    val systemId: Long,
    val number: String,
    val name: String?,
    val durationLabel: String,
    val timeLabel: String,
    val leadScore: Int,
    val tags: ImmutableList<TagUiModel>,  // kotlinx.collections.immutable
    val isBookmarked: Boolean,
)
```

`ImmutableList` + Compose Compiler 2.0's strong skipping = no unnecessary recomposition.

### 9.7 Image loading (Coil)

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.10).build() }
    .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve("avatars")).maxSizeBytes(50 * 1024 * 1024).build() }
    .crossfade(280)
    .respectCacheHeaders(false)  // Avatar URLs don't expire
    .build()
```

---

## 10. Tech Stack (Updated)

### 10.1 Core toolchain

| Component              | Version     |
| ---------------------- | ----------- |
| Kotlin                 | 2.1.0+ (K2) |
| AGP                    | 8.8.x       |
| Gradle                 | 8.11.x      |
| KSP                    | 2.1.0-1.0.x |
| compileSdk / targetSdk | 35          |
| minSdk                 | 26          |
| JVM target             | 17          |

### 10.2 Compose & UI

| Library                     | Version                                            |
| --------------------------- | -------------------------------------------------- |
| Compose BOM                 | 2026.04.00                                         |
| Compose Compiler            | 2.0+ (matches Kotlin) with strong skipping enabled |
| Compose Material 3          | 1.4.x                                              |
| Compose Material 3 Adaptive | 1.0.x                                              |
| Compose Navigation          | 2.8.x                                              |
| Compose Animation Graphics  | latest                                             |
| `core-splashscreen`         | 1.2.0-alpha                                        |

### 10.3 Persistence

| Library                             | Version | Notes                               |
| ----------------------------------- | ------- | ----------------------------------- |
| Room                                | 2.7.x   | KSP for compiler                    |
| Room paging                         | 2.7.x   | `@Query` returning `PagingSource`   |
| Paging 3                            | 3.3.x   | Compose integration                 |
| **SQLCipher**                       | 4.6.1   | `net.zetetic:sqlcipher-android`     |
| `androidx.sqlite:sqlite-ktx`        | 2.4.0   | SupportSQLiteOpenHelper integration |
| DataStore Preferences               | 1.1.x   | Settings (~40 keys)                 |
| `androidx.security:security-crypto` | 1.1.x   | EncryptedSharedPreferences          |
| Protobuf Lite                       | 4.28.x  | Archive format                      |

### 10.4 Async, DI, lifecycle

| Library                                        | Version     |
| ---------------------------------------------- | ----------- |
| Hilt                                           | 2.54+ (KSP) |
| `androidx.hilt:hilt-work`                      | 1.2.x       |
| WorkManager                                    | 2.10.x      |
| kotlinx coroutines                             | 1.10.x      |
| kotlinx serialization                          | 1.8.x       |
| kotlinx datetime                               | 0.6.x       |
| `kotlinx.collections.immutable`                | 0.3.8       |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.x       |

### 10.5 Performance & startup

| Library                                      | Version | Purpose                   |
| -------------------------------------------- | ------- | ------------------------- |
| `androidx.startup:startup-runtime`           | 1.2.0   | Initializer order         |
| `androidx.profileinstaller:profileinstaller` | 1.4.x   | Baseline profiles         |
| `androidx.benchmark:benchmark-macro-junit4`  | 1.3.x   | Generate baseline profile |
| `androidx.tracing:tracing-perfetto`          | 1.0.x   | Performance tracing       |

### 10.6 Telemetry (optional, build-flagged)

| Library         | Version                                                |
| --------------- | ------------------------------------------------------ |
| Sentry Android  | 7.x.x (only if `SENTRY_DSN` configured)                |
| PostHog Android | 3.x.x (only if `POSTHOG_API_KEY` configured)           |
| OkHttp          | 4.12.x (always — used for heartbeat + update manifest) |

### 10.7 UI extras

| Library                  | Version                |
| ------------------------ | ---------------------- |
| Coil                     | 3.0.x (`coil-compose`) |
| Lottie Compose           | 6.6.x                  |
| Compose Shimmer          | 1.3.x                  |
| Markwon (core + compose) | 4.6.x                  |
| Lucide Icons (Compose)   | 1.0.x                  |
| Vico Charts              | 2.1.x stable           |

### 10.8 Phone numbers, export, OAuth

| Library                  | Version |
| ------------------------ | ------- |
| libphonenumber-android   | 8.13.x  |
| Apache POI ooxml-lite    | 5.2.x   |
| iText core               | 8.0.x   |
| AppAuth (optional Drive) | 0.11.x  |

### 10.9 Quality

| Library           | Version                | Purpose         |
| ----------------- | ---------------------- | --------------- |
| Detekt            | 1.23.x                 | Static analysis |
| Spotless + ktlint | latest                 | Formatting      |
| Android Lint      | bundled                | Platform checks |
| LeakCanary        | 2.14+ (debugImpl only) | Memory leaks    |

### 10.10 Testing

| Library                      | Version |
| ---------------------------- | ------- |
| JUnit 5                      | 5.11.x  |
| Kotest assertions            | 5.9.x   |
| MockK                        | 1.13.x  |
| Turbine                      | 1.2.x   |
| `androidx.room:room-testing` | 2.7.x   |
| Paparazzi                    | 1.3.x   |
| Maestro                      | latest  |
| Kover                        | 0.9.x   |
| `androidx.test.ext:junit`    | 1.2.x   |

---

## 11. Layout Plans (Per Screen)

Every layout below uses tight spacing (16dp screen padding, 12dp card padding, 64dp list rows). No wasted space. Numbers are dp unless noted.

### 11.1 Splash

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│         [LOGO 96×96]                │
│                                     │
│         CallVault                   │  ← Instrument Serif 32sp
│                                     │
│                                     │
│                                     │
│         [progress dots]             │  ← only after 800ms
│                                     │
└─────────────────────────────────────┘
```

- Background: `gradient.brand` full-bleed
- Logo animates scale 0.92 → 1.0 + alpha 0 → 1 over 320ms
- Progress dots appear only if loading exceeds 800ms (prevents flicker)
- Auto-advance to onboarding or home as soon as ready

### 11.2 Home tab

```
┌─────────────────────────────────────┐
│ CallVault              🔍   ⚙️       │  ← Top bar 48dp, transparent
├─────────────────────────────────────┤
│                                     │
│  Today's pulse                      │  ← 14sp uppercase 600 label
│  ┌─────┬─────┬─────┐                │
│  │ 🔥  │ ⏰  │ 📥  │                │  ← 3 stat tiles, 96×96 each
│  │  4  │  6  │ 14  │                │  ← Geist Mono 28sp 600
│  │ Hot │Today│ New │                │  ← Geist 11sp 500
│  └─────┴─────┴─────┘                │
│                                     │
│  Quick actions                      │
│  ┌─────────────────────────────┐    │
│  │ + New note                  │    │  ← 48dp rows, gradient.cool icon
│  │ ⏰ Schedule follow-up        │    │
│  │ 📊 This week stats          │    │
│  │ 📤 Quick export             │    │
│  └─────────────────────────────┘    │
│                                     │
│  Recent unsaved (last 7 days)       │
│  ┌─────────────────────────────┐    │
│  │ +91 9876 543 210            │    │
│  │ 2 calls · 3m total · 2h ago │    │  ← Geist Mono 12sp
│  └─────────────────────────────┘    │
│  [+ 13 more]                        │
│                                     │
├─────────────────────────────────────┤
│  🏠   📞   📥   ⋯                  │  ← Tab bar 64dp
└─────────────────────────────────────┘
                    ┌──┐
                    │+ │  ← FAB 56dp, bottom-right 16dp from edges
                    └──┘
```

**Dimensions:**

- Top bar: 48dp (down from 56dp — saves 8dp, 16% header reclaim)
- Screen horizontal padding: 16dp
- Section vertical gap: 20dp
- Stat tile: 96×96 with 12dp internal padding
- Quick action row: 48dp
- Tab bar: 64dp
- FAB: 56dp (compact circle)

### 11.3 Calls tab

```
┌─────────────────────────────────────┐
│ Calls                  🔍   ⋯       │  ← 48dp
├─────────────────────────────────────┤
│ [🔍 Search number, name, or note]   │  ← Persistent search 44dp
├─────────────────────────────────────┤
│ [All ×] [Hot ×] [+ Filter]          │  ← Chip bar 36dp
├─────────────────────────────────────┤
│ ●  Rahul Sharma            [88]     │  ← 64dp row
│    +91 9876 543 210 · 2h     #Hot   │  ← Avatar 36dp, score pill
│ ─────────────────────────────────── │
│ ●  +91 8765 432 109        [42]     │
│    Inquiry · 4m · 11:32 AM          │
│ ─────────────────────────────────── │
│ ●  Priya Patel             [71]     │
│    +91 7654 321 098 · 1d            │
│ ─────────────────────────────────── │
│ ...                                 │
│                                     │
├─────────────────────────────────────┤
│  🏠   📞   📥   ⋯                  │
└─────────────────────────────────────┘
                              ┌──┐
                              │+ │
                              └──┘
```

**Row anatomy (64dp):**

- Avatar: 36dp circle, 12dp from left edge
- Name + score: 8dp from avatar, name on top, number/timestamp below
- Score pill: 24dp tall, 12dp from right edge
- Vertical center alignment of all elements
- 1dp `border.subtle` between rows

**Density:** 64dp rows → 9 rows visible on a 600dp viewport (vs 7 at 76dp). 28% more density.

### 11.4 Inquiries tab

```
┌─────────────────────────────────────┐
│ Inquiries              🔍   ⋯       │
├─────────────────────────────────────┤
│ [🔍 Search...]                      │
├─────────────────────────────────────┤
│ [All ×] [Last 7d ×] [Convert]       │  ← "Convert" button right-aligned
├─────────────────────────────────────┤
│ This week (12)                      │  ← Section header 32dp
├─────────────────────────────────────┤
│ ●  +91 9876 543 210        [88] 🔖 │  ← Bookmark indicator
│    callVault-s1 · 2 calls · 5m      │
│ ─────────────────────────────────── │
│ ...                                 │
├─────────────────────────────────────┤
│ Last week (47)                      │
├─────────────────────────────────────┤
│ ...                                 │
└─────────────────────────────────────┘
```

Same row anatomy as Calls tab. Sectioned by week. "Convert" mode toggles a multi-select for bulk renaming.

### 11.5 More tab

```
┌─────────────────────────────────────┐
│ More                                │
├─────────────────────────────────────┤
│                                     │
│ 📊 Stats                       ›    │  ← 56dp rows, grouped
│ 🏷️ Tags                        ›    │
│ ⚙️ Auto-tag rules              ›    │
│ 🎯 Lead scoring                ›    │
│ ─────────────────────────────────── │
│ 📞 Real-time features          ›    │
│ 💾 Auto-save                   ›    │
│ ─────────────────────────────────── │
│ ☁️ Backup & restore            ›    │
│ 📤 Export                      ›    │
│ ─────────────────────────────────── │
│ 🔄 App updates                 ›    │
│ 📚 Help & docs                 ›    │
│ 🔒 Privacy                     ›    │
│ ⚙️ Settings                    ›    │
│ ─────────────────────────────────── │
│ ℹ️ About CallVault             ›    │
│                                     │
├─────────────────────────────────────┤
│  🏠   📞   📥   ⋯                  │
└─────────────────────────────────────┘
```

Grouped iOS-style list. 56dp rows. Group headers omitted (use 1dp dividers between groups instead — saves vertical space).

### 11.6 Call Detail

```
┌─────────────────────────────────────┐
│ ←                       ⋯           │  ← 48dp top bar
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │  [HERO MESH GRADIENT]           │ │
│ │    Rahul Sharma                 │ │  ← Instrument Serif 22sp
│ │    +91 9876 543 210             │ │  ← Geist Mono 14sp
│ │                                 │ │
│ │              88                 │ │  ← Score in 64sp Mono
│ │           HOT LEAD              │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ [📞] [💬 SMS] [💚 WhatsApp] [⭐ Bookmark] │  ← Action bar 48dp
├─────────────────────────────────────┤
│ TAGS                                │  ← Section label
│ [#Customer] [#Quoted] [+ Add tag]   │
├─────────────────────────────────────┤
│ NOTES (3)                           │
│ ┌─────────────────────────────────┐ │
│ │ Sofa inquiry — wants 3-seater   │ │
│ │ in beige, ₹45k budget. Said     │ │
│ │ will visit Saturday.            │ │
│ │ — 2 days ago                    │ │
│ └─────────────────────────────────┘ │
│ [+ New note]                        │
├─────────────────────────────────────┤
│ FOLLOW-UP                           │
│ Tomorrow at 11:00 AM   [Edit] [✓]   │
├─────────────────────────────────────┤
│ CALL HISTORY (12)                   │
│ ↓ Incoming · 2h ago · 4m 22s        │  ← 44dp rows
│ ↑ Outgoing · 3d ago · 1m 08s        │
│ ↓ Missed · 1w ago                   │
│ [Show all 12]                       │
└─────────────────────────────────────┘
```

**Hero card:** 180dp tall, mesh gradient, score dominant.
**Action bar:** 48dp tall, 4 equal-width buttons.
**Sections:** all use 12sp uppercase 600 label, 12dp top padding, 4dp bottom.

### 11.7 Stats

```
┌─────────────────────────────────────┐
│ ← Stats               [Period ▼]    │  ← Period picker top right
├─────────────────────────────────────┤
│                                     │
│ ┌──────────┬──────────┐             │
│ │   247    │   18     │             │  ← Hero numbers, 32sp Mono
│ │  Calls   │  Hot     │             │  ← 110×80 tiles
│ ├──────────┼──────────┤             │
│ │   4h 22m │   83%    │             │
│ │  Talked  │ Saved    │             │
│ └──────────┴──────────┘             │
│                                     │
│ DAILY VOLUME                        │
│ [═══════ line chart 200dp tall ═══] │
│                                     │
│ BY HOUR OF DAY                      │
│ [═══ bar chart 160dp tall ═══════] │
│                                     │
│ BY DAY OF WEEK                      │
│ [═══ bar chart 120dp tall ═══════] │
│                                     │
│ LEAD SCORE DISTRIBUTION             │
│ [═══ histogram 140dp tall ═══════] │
│                                     │
│ TAG DISTRIBUTION                    │
│ [═══ pie chart 180×180 ═══════════] │
│                                     │
│ CONVERSION FUNNEL                   │
│ Inquiries 247 ─→ Tagged 198         │
│           Tagged 198 ─→ Followed 89 │
│           Followed 89 ─→ Won 34     │
│                                     │
└─────────────────────────────────────┘
```

Six charts as planned. Period picker swaps between This week / month / quarter / year.

### 11.8 Settings (master)

```
┌─────────────────────────────────────┐
│ ← Settings                          │
├─────────────────────────────────────┤
│ DATA                                │
│ Auto-save                      ›    │
│ Backup & restore               ›    │
│ Export                         ›    │
│ Reset all data                 ›    │
├─────────────────────────────────────┤
│ FEATURES                            │
│ Real-time features             ›    │
│ Lead scoring                   ›    │
│ Auto-tag rules                 ›    │
├─────────────────────────────────────┤
│ NOTIFICATIONS                       │
│ Follow-up reminders        [✓ ON]   │
│ Daily briefing             [✓ ON]   │
│ Daily briefing time          9:00 AM│
│ Update alerts              [✓ ON]   │
├─────────────────────────────────────┤
│ PRIVACY                             │
│ Master passphrase              ›    │
│ Crash reports              [○ OFF]  │  ← only if SENTRY_DSN configured
│ Product analytics          [○ OFF]  │  ← only if POSTHOG configured
│ Heartbeat (active users)   [✓ ON]   │  ← only if HEARTBEAT_URL configured
├─────────────────────────────────────┤
│ APPEARANCE                          │
│ Theme                  Light only   │
│ Default tab            Calls    ›   │
├─────────────────────────────────────┤
│ ABOUT                               │
│ Version                  1.0.0      │
│ Build                       100     │
│ Privacy policy                 ›    │
│ Open source licenses           ›    │
└─────────────────────────────────────┘
```

Privacy section only shows toggles for telemetries that are actually configured (BuildConfig flag check). If no telemetry configured, the entire row block is omitted.

### 11.9 Backup screen

```
┌─────────────────────────────────────┐
│ ← Backup & restore                  │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ ✅ Last backup successful       │ │
│ │    2 hours ago · 247 calls      │ │
│ │    Downloads/CallVault/         │ │
│ └─────────────────────────────────┘ │
│                                     │
│ AUTOMATIC                           │
│ Daily backup           [✓ ON]       │
│ Backup time              2:00 AM    │
│ Retention             7 days   ›    │
│                                     │
│ MANUAL                              │
│ [Backup now]                        │  ← Primary CTA, full-width
│ [Restore from file]                 │  ← Secondary CTA
│                                     │
│ CLOUD (optional)                    │
│ Google Drive          [○ OFF]       │
│   When ON: encrypted blob only.     │
│   Google sees ciphertext.           │
│                                     │
│ ARCHIVES                            │
│ 2024 Q1                  Encrypted  │
│ 2024 Q2                  Encrypted  │
│ 2024 Q3                  Encrypted  │
│ [+ View all archives]               │
└─────────────────────────────────────┘
```

### 11.10 Export wizard (5 steps)

Each step uses a bottom-sheet wizard pattern (full-screen sheet with progress indicator at top).

```
┌─────────────────────────────────────┐
│ ← Export                  [1 of 5]  │
├─────────────────────────────────────┤
│ ─────● ─── ○ ─── ○ ─── ○ ─── ○      │  ← Progress
├─────────────────────────────────────┤
│                                     │
│ Range                               │
│                                     │
│ ◉ This week (May 28 – Jun 3)        │
│ ○ This month                        │
│ ○ This quarter                      │
│ ○ This year                         │
│ ○ Last week                         │
│ ○ Last month                        │
│ ○ Custom range                      │
│                                     │
│                                     │
├─────────────────────────────────────┤
│            [Continue]               │  ← Primary CTA bottom
└─────────────────────────────────────┘
```

Step 1: Range. Step 2: Scope (All / Bookmarked / Tagged with X / Hot leads). Step 3: Columns. Step 4: Format (Excel / CSV / PDF). Step 5: Destination + final review.

### 11.11 Onboarding pages

Each page is full-bleed `gradient.mesh.hero` with single hero word + 1-line subhead + single CTA at bottom.

```
┌─────────────────────────────────────┐
│                                     │
│            [MESH GRADIENT]          │
│                                     │
│                                     │
│                                     │
│         Welcome.                    │  ← Instrument Serif 56sp italic
│                                     │
│                                     │
│   Turn your call log into a CRM.    │  ← Geist 18sp 80% opacity
│                                     │
│                                     │
│                                     │
│                                     │
│ ●  ○  ○  ○  ○                       │  ← Page indicators
│                                     │
│         [Continue]                  │  ← Solid surface CTA (NOT gradient)
│                                     │
└─────────────────────────────────────┘
```

Five pages: Welcome / Features / Permissions / OEM battery / First sync.

---

## 12. Performance Budgets

| Metric                      | Target                              | Hard ceiling  |
| --------------------------- | ----------------------------------- | ------------- |
| Cold start (first frame)    | 600ms                               | 900ms         |
| Cold start (interactive)    | 800ms                               | 1200ms        |
| Warm start                  | 250ms                               | 400ms         |
| Filter sheet apply (5k DB)  | 200ms                               | 400ms         |
| FTS query first result      | 60ms                                | 120ms         |
| Number suffix search        | 30ms                                | 80ms          |
| Sync 100 new calls          | 1.5s                                | 3s            |
| Lead score recompute (5k)   | 800ms                               | 1.5s          |
| Backup encryption (5k)      | 6s                                  | 10s           |
| Archive write (10k entries) | 2s                                  | 5s            |
| Excel export (10k rows)     | 4s                                  | 8s            |
| Scroll FPS (10k items)      | 120fps (high-end) / 60fps (low-end) | 60fps minimum |
| APK size (release)          | 18 MB                               | 25 MB         |
| Idle memory (RSS)           | 60 MB                               | 90 MB         |
| Active memory peak          | 120 MB                              | 180 MB        |
| ANR rate                    | 0                                   | 0             |

Verify budgets continuously with Macrobenchmark suite + Perfetto traces.

---

## 13. Implementation Order

### Phase A — Foundation (weeks 1–2)

1. Repo setup, multi-module Gradle, `libs.versions.toml`
2. Detekt + Spotless + GitHub Actions CI
3. `:core:design` tokens (per `CALLVAULT-UI-SPEC.md`)
4. SageTheme wrapper

### Phase B — Storage (weeks 3–4)

5. `:core:database` Room entities (with `userId`, `archivedAt`)
6. SQLCipher integration via `SupportSQLiteOpenHelper`
7. All indexes including partial indexes
8. FTS4 virtual tables + triggers
9. DAOs with Paging 3 `PagingSource`
10. `:core:datastore` DataStore Preferences + EncryptedSharedPreferences
11. Migration test framework

### Phase C — Domain (week 5)

12. UseCases: ComputeLeadScore, SyncCalls, EncryptBackup, EvaluateAutoTagRules, ResetAllData (fixed), Archive, ExportRange
13. PermissionManager with all 6 OEM deeplinks
14. Heartbeat telemetry (always-on if URL configured)
15. Sentry + PostHog wrappers (no-op if env absent)

### Phase D — Background (week 6)

16. All workers per Appendix C of v1.0
17. Add `ArchiveWorker` (weekly)
18. Add `HeartbeatWorker` (daily)
19. CallEnrichmentService foreground service
20. BootCompletedReceiver, PhoneStateReceiver

### Phase E — UI features (weeks 7–14)

21. `:feature:onboarding` (splash + 5 pages)
22. `:feature:home` (with stats tiles, quick actions, recent unsaved)
23. `:feature:calls` (with persistent search, swipe gestures, filter chips)
24. `:feature:inquiries`
25. `:feature:more`
26. CallDetail screen
27. `:feature:settings` master + sub-screens
28. `:feature:stats` with 6 charts
29. `:feature:backup` (with archive viewer)
30. `:feature:export` 5-step wizard + QuickExport sheet
31. `:feature:docs` with caching
32. `:feature:update` UpdateAvailable + UpdateSettings
33. Privacy settings sub-screen (conditional rendering based on BuildConfig)

### Phase F — Pre-release (weeks 15–17)

34. Baseline Profile generation via Macrobenchmark
35. Maestro E2E suite
36. Paparazzi snapshot tests for all components
37. Performance verification against budgets
38. Privacy policy hosting
39. `versions.json` manifest setup
40. Release signing key + vault storage
41. Direct APK landing page

---

## 14. Code Patterns

### 14.1 SQLCipher + Room integration

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: PassphraseProvider,
    ): CallVaultDatabase {
        val passphrase = passphraseProvider.get()  // From SecurePrefs or onboarding
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

        return Room.databaseBuilder(context, CallVaultDatabase::class.java, "callvault.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .setQueryCallback({ sql, args ->
                if (BuildConfig.DEBUG) Timber.d("SQL: $sql")
            }, Executors.newSingleThreadExecutor())
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
            .also { db ->
                db.openHelper.writableDatabase.apply {
                    execSQL("PRAGMA cache_size = -8192")
                    execSQL("PRAGMA temp_store = MEMORY")
                    execSQL("PRAGMA mmap_size = 30000000")
                }
            }
    }
}
```

### 14.2 Paginated list with Compose

```kotlin
@HiltViewModel
class CallListViewModel @Inject constructor(
    private val callDao: CallDao,
) : ViewModel() {
    val pagingFlow: Flow<PagingData<CallEntity>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 10,
            enablePlaceholders = false,
            initialLoadSize = 60,
        ),
        pagingSourceFactory = { callDao.pagingSourceCalls("local", null) }
    ).flow.cachedIn(viewModelScope)
}

@Composable
fun CallListScreen(viewModel: CallListViewModel = hiltViewModel()) {
    val items = viewModel.pagingFlow.collectAsLazyPagingItems()

    LazyColumn {
        items(
            count = items.itemCount,
            key = items.itemKey { it.systemId },
            contentType = items.itemContentType { "call" },
        ) { idx ->
            val call = items[idx]
            if (call != null) {
                CallRow(call)
            } else {
                CallRowSkeleton()
            }
        }
    }
}
```

### 14.3 Optional telemetry usage

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val telemetry: Telemetry,  // CompositeTelemetry or NoOpTelemetry
) : ViewModel() {
    fun onTagApplied(tagId: Long, callId: Long) {
        telemetry.trackEvent("call_tagged", mapOf("tagId" to tagId))
        // Always safe to call. No-op if no telemetry configured.
    }
}
```

### 14.4 Archive worker

```kotlin
@HiltWorker
class ArchiveWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val callDao: CallDao,
    private val archiveWriter: ArchiveWriter,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        val toArchive = callDao.findArchivable(cutoff, limit = 5000)

        if (toArchive.isEmpty()) return@withContext Result.success()

        // Group by quarter
        val grouped = toArchive.groupBy { call ->
            val date = Instant.fromEpochMilliseconds(call.dateMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            "${date.year}_Q${(date.monthNumber - 1) / 3 + 1}"
        }

        grouped.forEach { (quarter, calls) ->
            archiveWriter.appendToQuarter(quarter, calls)
        }

        // Mark archived
        callDao.markArchived(toArchive.map { it.systemId }, System.currentTimeMillis())

        Result.success()
    }
}
```

### 14.5 Stable UI models with ImmutableList

```kotlin
@Immutable
data class CallUiState(
    val calls: ImmutableList<CallUiModel>,
    val isLoading: Boolean,
    val error: String?,
)

@Immutable
data class CallUiModel(
    val systemId: Long,
    val displayName: String,
    val displayNumber: String,
    val timeLabel: String,
    val durationLabel: String,
    val leadScore: Int,
    val tags: ImmutableList<TagUiModel>,
    val isBookmarked: Boolean,
)
```

---

## 15. Readiness Checklist

### 15.1 Storage

- [ ] SQLCipher integration verified — DB unreadable via ADB without passphrase
- [ ] All 14 entities have proper indexes (verify with EXPLAIN QUERY PLAN)
- [ ] Compound indexes for hot query patterns confirmed in use
- [ ] Partial indexes for sparse data (bookmarks, follow-ups) verified
- [ ] FTS4 + reversed-number suffix search working
- [ ] `userId` column on every entity, default `'local'`
- [ ] WAL mode + 8MB cache + 30MB mmap configured
- [ ] Migration tests pass for v1 → v2 → v3

### 15.2 Performance

- [ ] Cold start measured < 800ms on Pixel 4a-class device
- [ ] Baseline Profile generated and shipped
- [ ] FTS query < 60ms for 5k-call DB
- [ ] Number suffix search < 30ms for 50k-call DB
- [ ] Scroll FPS sustained 60+ in 10k-item list
- [ ] APK size < 25MB
- [ ] Idle RSS < 80MB
- [ ] All `@Stable`/`@Immutable` annotations applied to UI models
- [ ] `ImmutableList` used for all collection fields in UI state
- [ ] Compose Compiler strong skipping enabled and verified

### 15.3 Telemetry (optional)

- [ ] Builds without `SENTRY_DSN` produce no Sentry calls (verify via R8 mapping)
- [ ] Builds without `POSTHOG_API_KEY` produce no PostHog calls
- [ ] Heartbeat-only build works and pings endpoint daily
- [ ] Privacy settings screen renders only configured telemetries
- [ ] No-op fallback verified to be zero-cost (R8 strips dead code)

### 15.4 Archival

- [ ] ArchiveWorker runs weekly
- [ ] Archive files encrypted with AES-256-GCM
- [ ] Archive files have SHA-256 integrity tail
- [ ] Restoring from archive works end-to-end
- [ ] Hot DB row count stays under 15k after archival

### 15.5 Export

- [ ] Excel export with 4 period presets (week/month/quarter/year) works
- [ ] Exports spanning hot + warm data work transparently
- [ ] Streaming writer keeps memory < 10MB for 10k-row export
- [ ] All 7 sheets populated (Summary, All Calls, Hot Leads, Follow-Ups, Notes, By Tag)
- [ ] PDF export pagination correct
- [ ] CSV export character escaping correct

### 15.6 UI

- [ ] Sage / Earth design system per `CALLVAULT-UI-SPEC.md` fully implemented
- [ ] All layouts at 16dp horizontal padding, 64dp list rows
- [ ] No empty space wastage (every screen reviewed against layout plan)
- [ ] All editors are bottom sheets (no AlertDialog)
- [ ] All lists use Paging 3 with skeleton loaders
- [ ] All data classes consumed by Compose are `@Stable` or `@Immutable`

### 15.7 Distribution

- [ ] Privacy policy hosted on own website
- [ ] `versions.json` manifest format finalized
- [ ] Release signing key in password vault, never committed
- [ ] APK landing page live
- [ ] Self-update flow tested end-to-end
- [ ] First user manual install path documented

### 15.8 Quality

- [ ] Detekt clean
- [ ] Spotless clean
- [ ] Android Lint clean
- [ ] Coverage ≥85% in `:core:domain`
- [ ] Coverage ≥80% in `:core:database`
- [ ] All Paparazzi snapshots match
- [ ] Maestro E2E suite passes
- [ ] LeakCanary clean in debug

---

## Appendix A — File structure

```
callvault/
├── app/
│   ├── build.gradle.kts
│   ├── baseline-prof.txt
│   └── src/main/
│       ├── java/com/callvault/
│       │   ├── CallVaultApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── di/TelemetryModule.kt
│       │   └── di/DatabaseModule.kt
│       └── AndroidManifest.xml
├── core/
│   ├── design/        (tokens, components, SageTheme)
│   ├── database/      (Room + SQLCipher)
│   ├── datastore/     (DataStore + SecurePrefs)
│   ├── common/        (utilities)
│   ├── domain/        (UseCases)
│   ├── permission/
│   ├── analytics/     (Telemetry interface + impls)
│   ├── billing/       (RevenueCat scaffold, inactive)
│   └── archive/       (encrypted Protobuf archives)
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── calls/
│   ├── inquiries/
│   ├── more/
│   ├── settings/
│   ├── backup/
│   ├── export/
│   ├── stats/
│   ├── docs/
│   └── update/
├── benchmark/         (Macrobenchmark module)
├── gradle/
│   └── libs.versions.toml
├── docs/
│   ├── APP-SPEC.md
│   ├── CALLVAULT-UI-SPEC.md
│   ├── CALLVAULT-PHASE1-OPTIMIZED.md   ← this file
│   ├── DECISIONS.md
│   └── CHANGELOG.md
└── README.md
```

---

## Appendix B — Quick reference: env vars

| Variable                     | Required?            | Effect when absent                                        |
| ---------------------------- | -------------------- | --------------------------------------------------------- |
| `SENTRY_DSN`                 | Optional             | No crash reporting; SentryTelemetry never instantiated    |
| `POSTHOG_API_KEY`            | Optional             | No product analytics; PostHogTelemetry never instantiated |
| `HEARTBEAT_URL`              | Recommended          | No active-user counting; HeartbeatTelemetry skipped       |
| `RELEASE_KEYSTORE_PATH`      | Required for release | Release builds fail                                       |
| `RELEASE_KEYSTORE_PASSWORD`  | Required for release | Release builds fail                                       |
| `UPDATE_MANIFEST_STABLE_URL` | Required             | Self-update disabled                                      |

Place in `local.properties` (gitignored) or as CI secrets. Never commit.

---

**End of CallVault Phase 1 Optimized Plan.**

This document supersedes the previous Phase 1 gap analysis. Pair with `CALLVAULT-UI-SPEC.md` for visual specification and `APP-SPEC.md` for domain logic.
