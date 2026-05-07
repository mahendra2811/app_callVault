# callNest — AI handoff docs

Read these in order. The first three give a future cold-start session everything it needs to be productive without exploring the source.

## Cold-start reading order (read these 3 first)

1. **`PROJECT_MAP.md`** — one-page tree of `app/src/main/`, layering rules, where each kind of new thing goes. Replaces aimless `find`/`tree` calls.
2. **`UI_GUIDE.md`** — `Neo*` component inventory, layout primitives (`StandardPage`, `NeoScaffold`), color tokens, and a screen-by-screen layout sketch. Read this before building any Compose surface.
3. **`AUDIT_2026-05-06.md`** — current open bugs, dead code, broken nav, and the UI-direction recommendation. The single source of truth for "what's actually wrong right now". Always reconcile against `git log` since 2026-05-06 — entries may be already fixed.

## When you need depth

4. `01-project-brief.md` → product, personas, status, tech stack.
5. `02-architecture-and-flows.md` → app startup, sync pipeline, real-time hooks, export, backup, updates.
6. `03-file-map.md` → file-by-file index (longer than `PROJECT_MAP.md`).
7. `FEATURE_BACKLOG.md` → ranked, checkbox-edit-able feature backlog from architect review.
8. `04-future-plan-and-backlog.md` → older sprint-era backlog (kept for history; `FEATURE_BACKLOG.md` supersedes it).
9. `05-ai-working-guide.md` → behavioural rules for agents (string-resource-only, Timber-only, no `TODO(`, etc.).
10. `06-glossary.md` → domain vocabulary (inquiry, lenient bucket, orphan note, lead score buckets).
11. `07-mcp-guide.md` → Context7 + Playwright MCP usage.
12. `08-agent-spawn-templates.md` → copy-paste sub-agent prompts (`callNest-android-engineer`, `callNest-ui-builder`, etc.).

## Spec & out-of-tree references

- **Spec (locked)**: `/home/primathon/Downloads/callNest_mega_prompt.md` (1533 lines). Use Read with offset/limit; CLAUDE.md lists section ranges.
- **`CLAUDE.md`** (repo root) → session rules. Note: §"Don'ts" was reversed 2026-05-05 — Supabase Auth, PostHog, FCM are **now in scope**. See `DECISIONS.md` "Cloud pivot".
- **`DECISIONS.md`** → every fallback, deferral, trade-off (read before re-litigating any architectural choice).
- **`docs/architecture.md`**, **`docs/cloud-integration.md`**, **`docs/env-setup.md`** → human-facing engineering docs (not AI-tuned).
- **`TODO.md`** → P0–P3 punch list, hand-edited.
- **`CHANGELOG.md`** → what each version added.

## Important snapshot (2026-05-06)

- **245 Kotlin files** in `app/src/main/`, 13 sprints shipped.
- **3 unit tests, 0 instrumentation tests** — testing is a known debt.
- **Active development phase**: `./gradlew` builds + `adb install` are pre-authorized for every code change. Don't ask.
- **Auth is now app-wide gate**: `Destinations.Login` is reached before onboarding/permissions if `AuthState.SignedOut`. A whole secondary auth graph (`AuthNavGraph.kt`) exists but is **not wired** — see `AUDIT_2026-05-06.md`.
- **UI design system**: custom neumorphic ("Neo*") in `ui/components/neo/` + `ui/theme/Neo*`. Recommendation in `AUDIT_2026-05-06.md` § "UI direction".

## Documentation hygiene rule

When you fix something listed in `AUDIT_2026-05-06.md`, strike it through (`~~text~~`) rather than deleting. When you add a new finding, append a dated note. When the audit is older than 30 days, schedule a re-audit before starting a feature push.
