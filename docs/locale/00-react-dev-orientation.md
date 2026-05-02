# 00 — React dev orientation

Mental model translation. Read this first.

## The single most important thing

**There is no `npm run dev`. There is no localhost. There is no hot-reload-in-browser.**

Android apps run on a phone or an emulator (a virtual phone). The "dev server" is replaced by a build-and-install loop:

```
edit code → build APK → install on phone → see changes
```

The closest thing to "instant feedback" is two tools:

1. **Compose Previews** — render any UI component in a side-panel inside Android Studio. Like Storybook, but built-in. Re-renders in <1 second on save. **This is what you'll use 80% of the time when working on UI.**
2. **Live Edit / Apply Changes** — once the app is installed on a phone, push code changes without restarting. Closest equivalent to React Fast Refresh.

## Term map

| React / Next.js | Android / CallVault |
|-----------------|---------------------|
| `npm run dev` | `./gradlew installDebug` (one-shot install on phone), then **Live Edit** for incremental updates |
| Browser at `localhost:3000` | A real Android phone plugged in via USB, or an emulator (Android Virtual Device) |
| Chrome DevTools console | **Logcat** in Android Studio (View → Tool Windows → Logcat) |
| `console.log("...")` | `Timber.d("...")` — already imported throughout this project |
| `console.error(err)` | `Timber.e(err, "message")` |
| React DevTools (component tree) | **Layout Inspector** (Tools → Layout Inspector) |
| Network tab | **Network Inspector** in App Inspection — but CallVault is offline-first, so almost nothing here |
| `package.json` | `app/build.gradle.kts` (per-module) + `gradle/libs.versions.toml` (version catalog) |
| `node_modules/` | `~/.gradle/caches/` — shared across projects, ~500 MB |
| `npm install` | First `./gradlew assembleDebug` (downloads everything) |
| `tsconfig.json` | None per se — `build.gradle.kts` has the same role plus more |
| `.env` | Mostly `BuildConfig.X` fields declared in `build.gradle.kts`; secrets via `keystore.properties` (not committed) |
| JSX | **Composables** — `@Composable fun X() { ... }` — same idea, render functions |
| `useState` | `remember { mutableStateOf(...) }` |
| `useEffect` | `LaunchedEffect(key) { ... }` |
| Context API | Hilt dependency injection (much more like Inversify or NestJS DI) |
| Tailwind classes | `Modifier` chains: `Modifier.padding(16.dp).background(...)` |
| CSS variables | Theme tokens: `MaterialTheme.colorScheme.primary`, `NeoColors.Base` |
| Server actions | Use cases — `domain/usecase/{Verb}{Noun}UseCase.kt` |
| API routes (`/api/...`) | Repositories + DAOs (Room SQL queries on-device) |
| Database (Postgres / Mongo) | **Room** — local SQLite. There is no remote DB; everything is on the phone. |
| `localStorage` | **DataStore Preferences** — typed key-value, on-device |
| Cookies / sessions | None — single-user offline app |
| Server components | Background workers (`WorkManager`) for sync / backup / update checks |
| Vercel / Netlify | Sideloaded APK from a website. No hosting platform. |
| `vercel.json` | None |
| `next build` | `./gradlew assembleRelease` |
| `next start` (production) | An installed signed APK on the user's phone |

## Mental model: "this app is more like an Electron app than a Next.js app"

CallVault is a **single-user, single-device, offline-first** app. The user's phone is the entire stack:

- The "frontend" is Compose UI.
- The "backend" is also on the phone — Room (SQLite), DataStore, `WorkManager`, ContentProvider reads of the device's call log and contacts.
- The "server" — for self-update only — is just a static `versions.json` file hosted somewhere (e.g. GitHub Pages). Nothing else leaves the device.

Forget about routing, requests, deployments, environments, multi-tenant logic. None of it applies.

## Things that are easier than the web

- **No browser compatibility.** One renderer (Compose), one platform (Android 26+).
- **No CSS specificity hell.** Modifiers compose left-to-right, deterministically.
- **No bundler config.** Gradle handles it.
- **No CORS.**
- **One deploy target** (the phone).

## Things that are harder than the web

- **Build times.** First build: 3–8 minutes. Incremental: 30–90 seconds. Live Edit only works for pure-Compose changes.
- **You need a phone.** An emulator works for layout, but the call-log features need a real device with real call history.
- **Permissions are runtime + Settings-app-level.** Some need the user to dig into system Settings.
- **No `view-source`.** You inspect via logcat + Layout Inspector + Database Inspector. They're powerful but require Android Studio.
- **No `npm install <new-thing>` casually.** Every new dep means editing `libs.versions.toml` + `app/build.gradle.kts`, then a Gradle sync.

## What you can carry over from React

- **Component thinking.** A Composable function takes state in (props/lambdas) and returns UI. Same shape as a React functional component.
- **Unidirectional data flow.** State flows down, events flow up via lambdas. Same pattern.
- **Hooks-style API.** `remember {}` ≈ `useMemo`, `LaunchedEffect` ≈ `useEffect`, `produceState` ≈ `useEffect`+`useState` combo.
- **Reactivity.** Compose recomposes on state change just like React re-renders.
- **TypeScript instincts.** Kotlin's type system is strictly stronger. If you write idiomatic TypeScript, your Kotlin will feel familiar.

## What's wildly different

- **No virtual DOM.** Compose builds a tree of layout nodes directly. You won't see "rendered N times" anti-patterns the same way; the rules differ.
- **Coroutines instead of Promises.** `suspend fun` is like `async function` — but cancellation is cooperative and built into the type system. `Flow<T>` is like an async iterable / RxJS Observable; this is the project's main reactivity primitive (think SWR + Zustand combined).
- **DI is everywhere.** Hilt (Google's wrapper around Dagger) injects every ViewModel, repository, and use case. You won't `import { db } from '@/lib/db'` — you'll add `@Inject constructor(private val callRepo: CallRepository)`.
- **No ESLint warnings clutter — but Lint is still strict.** `./gradlew lint` produces an HTML report at `app/build/reports/lint-results.html`.

## Next: open `01-setup.md` to install the toolchain.
