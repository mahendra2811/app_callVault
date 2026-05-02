---
description: Onboarding-from-cold-start — load the project context (CLAUDE.md, .claude/docs/, TODO.md, recent CHANGELOG entry) so Claude is ready to continue. Run this once at the start of any new session.
---

Load CallVault project context for a fresh session.

Steps (run in order, each output should fit on a screen):

1. Read CLAUDE.md (the project root one):
   ```bash
   cat "/home/primathon/Documents/p_projet/a_APP/4. callVault/CLAUDE.md"
   ```

2. Read the docs index:
   ```bash
   ls "/home/primathon/Documents/p_projet/a_APP/4. callVault/.claude/docs/"
   cat "/home/primathon/Documents/p_projet/a_APP/4. callVault/.claude/docs/README.md"
   ```

3. Read the latest CHANGELOG entry (last v):
   ```bash
   awk '/^## /{i++} i==1' "/home/primathon/Documents/p_projet/a_APP/4. callVault/CHANGELOG.md"
   ```

4. Read TODO.md:
   ```bash
   cat "/home/primathon/Documents/p_projet/a_APP/4. callVault/TODO.md"
   ```

5. Show last-modified files (clue to where work stopped):
   ```bash
   find "/home/primathon/Documents/p_projet/a_APP/4. callVault/app/src/main" -name "*.kt" -newer "/home/primathon/Documents/p_projet/a_APP/4. callVault/CHANGELOG.md" 2>/dev/null | head -10
   ```

6. Summarize for the user in 4–6 lines: "Project status, last sprint, top 3 P0 items in TODO, suggested next move." End with: "Ready. Run `/next` to start the next item, or tell me what to do."
