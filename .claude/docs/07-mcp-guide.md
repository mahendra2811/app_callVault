# 07 — MCP Servers (Context7 + Playwright)

CallVault's `.mcp.json` registers two project-scoped MCP servers. Both are **optional helpers** — the app builds and runs without them — but they speed up specific tasks for AI agents working in this repo.

## Files

- `.mcp.json` — server definitions, committed to the repo. Auto-loads when Claude Code opens this folder.
- `.claude/settings.json` — controls permissions (this file is also project-scoped).

## Servers

### Context7 — live library documentation

**What it does**: fetches up-to-date docs for any library, framework, SDK, or CLI. Resolves a name like "Compose Navigation" to a canonical library id (`/jetbrains/compose-navigation`), then fetches live API references and code samples.

**When to use**:
- "How do I use the new `PullToRefreshBox` API in Material 3?" — much faster than guessing or reading the source.
- Migration questions across versions of Hilt / Room / Compose / WorkManager.
- Verifying iText 8 / POI / Tink API surface (these were spec-locked years before some of Claude's training cutoffs).
- Library-specific debugging (e.g. "Why does `AssistedInject` fail with this error?").

**When NOT to use**:
- General Kotlin questions ("how do I write a lambda?") — your training is already current enough.
- Refactoring work or business-logic debugging — Context7 doesn't know the codebase.
- Broad searches ("show me all Android networking libs") — too vague to be useful.

**Tools available** (when the server is connected):
- `resolve-library-id` — turn a name into a `/org/project` id.
- `query-docs` — fetch docs for a resolved id with a specific question.

**Usage pattern** (paraphrasing the typical flow):
```
1. mcp__context7__resolve-library-id (libraryName: "Apache POI")
   → returns multiple matches; pick by relevance + description
2. mcp__context7__query-docs (
     libraryId: "/apache/poi",
     userQuery: "How do I create a multi-sheet workbook with bold headers?"
   )
   → returns docs + code snippets
```

**Tips**:
- Be specific in the query. "How do I configure Hilt for WorkManager" beats "Hilt".
- If results look wrong, try alternate names ("next.js" not "nextjs"; "compose multiplatform" not "compose").
- If quota errors appear, the user can run `npx ctx7 login` or set `CONTEXT7_API_KEY`.

### Playwright — browser automation

**What it does**: drives a real browser (Chromium by default) to navigate URLs, fill forms, click, screenshot, scrape DOM, and run scripts.

**When to use** (in this repo):
- **Hosting the update manifest**: when the user stands up a `versions-stable.json` page (e.g. on GitHub Pages), Playwright can verify the file is reachable, parses cleanly, and returns the right `Content-Type`.
- **Visual regression for the in-app docs landing page** if/when one is added.
- **Smoke-testing a release notes page** before bumping the manifest version.
- **Capturing screenshots** of any web-hosted material that ends up in the README or marketing.
- **Scraping OEM vendor pages** for updated autostart deep-link components when manufacturers change paths between OS versions.

**When NOT to use**:
- Anything Android-specific — Playwright drives browsers, not phones. Use `adb` for phones.
- General web research — Firecrawl/WebFetch is fine for one-shots.

**Tools available** (when the server is connected) include:
- `browser_navigate`, `browser_click`, `browser_type`, `browser_fill`
- `browser_snapshot`, `browser_take_screenshot`
- `browser_evaluate` (run JS on the page)
- `browser_network_requests` (inspect XHRs)
- `browser_console_messages` (inspect console)

**Usage pattern**:
```
browser_navigate(url: "https://callvault.app/dl/versions-stable.json")
browser_evaluate(function: "() => document.body.innerText")
   → confirm valid JSON, correct schema
```

## How they're wired

`.mcp.json` at the project root registers both servers as `stdio` transports invoked via `npx`:

```json
{
  "mcpServers": {
    "context7": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp"]
    },
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@playwright/mcp@latest"]
    }
  }
}
```

When a Claude Code session opens this folder, the user is prompted (per the project settings) to approve each MCP server. After approval the tools become available with the prefix `mcp__context7__*` and `mcp__playwright__*`.

## First-time setup checklist

1. Ensure Node ≥ 20 is on PATH (`node -v`). `npx` ships with Node.
2. Open Claude Code in this project. It detects `.mcp.json` and prompts for approval.
3. For Playwright: the first invocation downloads ~300 MB of Chromium. If the user is offline, Playwright fails gracefully — surface the error and suggest a connected machine.
4. For Context7: free tier is rate-limited. If quota errors appear, the user can run `npx ctx7 login` or set `CONTEXT7_API_KEY`.

## Don'ts

- **Don't browse user-private URLs with Playwright unprompted.** Playwright can run JS against any page; treat the user's data perimeter respectfully. Confirm before navigating to `localhost:*`, internal tools, or anything authenticated.
- **Don't paste sensitive strings (API keys, passphrases, PII) into Context7 queries.** They go to a third-party service.
- **Don't lean on Context7 for project-specific debugging.** It doesn't know CallVault. Use Read + Grep against the codebase.

## Removing them

If a contributor objects to either server, they can:
- Remove the entry from `.mcp.json`, or
- Click "Disable" in the Claude Code MCP-server prompt on first open.

The build is unaffected.
