---
description: Pick the highest-priority unchecked item from TODO.md and work on it. Routes to the appropriate subagent.
---

Resume CallVault work. Pick the next item from `TODO.md`.

Steps:

1. Read `TODO.md`:
   ```bash
   cat "/home/primathon/Documents/p_projet/a_APP/4. callVault/TODO.md"
   ```

2. Find the highest-priority unchecked item:
   - First scan P0 — if any unchecked, pick the first.
   - Then P1, P2, P3.

3. Echo the chosen item to the user (one sentence) and confirm before starting:
   > "Next item: '{item}'. Proceed?"

4. On confirmation, route to the right subagent:
   - **Build/lint errors** → `callvault-build-fixer`
   - **Compose UI work** (new screens, accessibility, empty states) → `callvault-ui-builder`
   - **Tests** → `callvault-test-writer`
   - **Documentation** (CHANGELOG, DECISIONS, README, in-app docs) → `callvault-doc-writer`
   - **Audit/gap analysis** → `callvault-spec-reviewer`
   - **Multi-layer feature work** → `callvault-android-engineer`

5. After the subagent finishes:
   - Mark the item checked in `TODO.md`.
   - Append any newly-discovered follow-ups to `TODO.md` under the right priority tier.
   - Append to `CHANGELOG.md` if user-visible.
   - Append to `DECISIONS.md` if a fallback was taken.
   - Report a one-paragraph summary.

6. Ask the user if they want to continue with the next item or stop.
