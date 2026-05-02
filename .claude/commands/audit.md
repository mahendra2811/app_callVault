---
description: Run a full spec-vs-code audit and produce a ranked production-readiness report. Read-only — does not modify any file.
---

Audit CallVault for production readiness.

Spawn the `callvault-spec-reviewer` agent with this prompt:

> "Audit CallVault for production readiness. Project root: `/home/primathon/Documents/p_projet/a_APP/4. callVault/`. Spec: `/home/primathon/Downloads/callvault_mega_prompt.md`. Read CLAUDE.md, then audit every spec subsection §3.1–§3.25 plus the §0 quality bar. Cross-check `DECISIONS.md` so intentional deferrals don't get re-flagged. Output a markdown report with: a summary count (shipped/partial/missing/deferred), a ranked punch-list (P0–P3), and a per-section status table. Do NOT modify any file."

After the agent reports, save its output to `audit-{YYYY-MM-DD}.md` in the project root for the user's reference, and surface the P0 list in the chat.
