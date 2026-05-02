# CallVault AI Docs

These docs are written for future AI agents working inside this project. Start here, then open only the specific document needed for the task.

## Reading Order

1. `01-project-brief.md` - what CallVault is, who it serves, status, tech stack, constraints.
2. `02-architecture-and-flows.md` - layers, app startup, navigation, sync, auto-save, rules, real-time, export, backup, updates.
3. `03-file-map.md` - where each project file or file group lives and what it owns.
4. `04-future-plan-and-backlog.md` - pending work, future plans, risk list, release path.
5. `05-ai-working-guide.md` - how future agents should answer, edit, test, and avoid mistakes in this repo.
6. `06-glossary.md` - domain terms (inquiry, lead, lenient bucketing, orphan note, etc.).
7. `07-mcp-guide.md` - Context7 + Playwright MCP servers wired via `.mcp.json`.
8. `08-agent-spawn-templates.md` - copy-paste templates for spawning each subagent.

## Project Summary

CallVault is a local-first Android call-log CRM for Indian small-business owners. It reads the device call log, enriches calls with contact and SIM data, auto-saves inquiry numbers into a dedicated contact group, supports tags, notes, bookmarks, follow-ups, lead scoring, exports, encrypted backups, real-time overlays, and self-update by sideloaded APK.

The app is intentionally sideloaded only:

- No Play Store dependency.
- No Google Play Services.
- No Firebase, analytics, telemetry, or crash reporting.
- Network use is limited to update manifest checks.
- User data stays on device unless the user exports or backs it up.

## Important Local References

- `CLAUDE.md` - repo rules loaded into Claude sessions.
- `README.md` - high-level product/build summary.
- `DEVELOPING.md` - local build, install, debug, verification guide.
- `docs/architecture.md` - existing architecture overview.
- `CHANGELOG.md` - shipped v1.0.0 features and known limitations.
- `DECISIONS.md` - trade-offs and deferred work.
- `app/src/main/assets/docs/` - 15 user-facing in-app help articles.

## Current Documentation Snapshot

Created from the project files on 2026-05-01. No `TODO.md` exists in this workspace at the time of writing, so pending work is derived from `CHANGELOG.md`, `DEVELOPING.md`, `DECISIONS.md`, and the code map.

