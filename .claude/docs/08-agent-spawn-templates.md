# 08 — Agent Spawn Templates

Copy-paste templates for spawning each subagent. The orchestrator (the main Claude session) owns scope and approval; each subagent owns execution. Templates here are tuned to keep agents under the stream-idle window (~10 min) by capping reads + writes.

## Template patterns — universal rules

Every spawn prompt should:

1. **State the scope in one paragraph.** No marketing language.
2. **Name the project root** with quotes (the path has a space + dot).
3. **Cap the read budget**. Past stalls correlate with over-reading.
4. **Tell the agent to skip `@Preview` first if running short**, never skip core logic.
5. **Forbid `./gradlew`, `gradle`, network commands.** The user runs builds.
6. **Require a terse report** (≤ 250 words): files written, files modified, deviations, next-step hint.

## Template — `callvault-android-engineer`

```
Implement <feature> for CallVault. Project root:
"/home/primathon/Documents/p_projet/a_APP/4. callVault/" (quote — space + dot).

Read CLAUDE.md, then read MAX 8 files (list them). Spec sections to consult by
offset/limit only:
- §3.<NN> ... at lines <offset>–<offset+limit>
- §8.<NN> ... at lines <offset>–<offset+limit>

DO NOT run gradle / network commands.

Deliverables:
1. <file 1 + spec>
2. <file 2 + spec>
...

Coding rules: KDoc on public APIs · @Preview on new composables · StateFlow +
collectAsStateWithLifecycle · Strings via stringResource · No mock data outside
@Preview · No NotImplementedError on user-reachable paths.

Time discipline: 30 minutes wall clock. Skip @Preview blocks first if short.

Report (≤ 250 words): files by directory, navigation routes added, deviations,
what's ready for the next sprint.
```

## Template — `callvault-ui-builder`

```
Build <screen / component> for CallVault. Project root:
"/home/primathon/Documents/p_projet/a_APP/4. callVault/".

Read CLAUDE.md and `.claude/docs/05-ai-working-guide.md`. Then read existing
sibling screens in ui/screen/<area>/ for convention. MAX 6 reads.

Use the Neo* component library — never raw Material 3 surfaces against the
neumorphic base.

Deliverables:
- <Screen>.kt (NeoScaffold + NeoTopBar + content)
- <Screen>ViewModel.kt (@HiltViewModel + StateFlow)
- Empty / loading / error states (NeoEmptyState)
- @Preview for empty + populated + error states

Strings via stringResource — append to res/values/strings.xml.

Accessibility: contentDescription on every NeoIconButton, 48dp touch targets,
WCAG AA contrast on lead-score badges.

Report (≤ 200 words): composables added, previews added, strings added,
accessibility items hit.
```

## Template — `callvault-test-writer`

```
Write tests for <ViewModel | use case | DAO> in CallVault. Project root:
"/home/primathon/Documents/p_projet/a_APP/4. callVault/".

Read the production file under test, the spec section behind it (§<NN>), and
1–2 existing tests for convention. MAX 6 reads.

Unit tests live in app/src/test/, instrumentation in app/src/androidTest/.
JUnit5 + MockK + Turbine + kotlinx-coroutines-test for unit; AndroidJUnit4 +
Room.inMemoryDatabaseBuilder for DAO instrumentation.

Cases to cover:
- <case 1>
- <case 2>
...

Don't mock things that are pure (LocalDateTime, data classes). Don't use
Thread.sleep.

Report (≤ 200 words): test files added, test count, cases hit, anything skipped
because the API didn't expose it.
```

## Template — `callvault-build-fixer`

```
Triage CallVault build errors. Project root:
"/home/primathon/Documents/p_projet/a_APP/4. callVault/".

Build output (paste below):
<<<
<full ./gradlew assembleDebug output>
>>>

Read CLAUDE.md and `.claude/agents/callvault-build-fixer.md`. Walk errors
top-down, fix one at a time. Read offending files (max 6).

DO NOT run gradle yourself — wait for the user to re-build after each fix.

If an error has multiple plausible fixes, surface options instead of guessing.

Report (≤ 200 words): errors fixed (with line refs), files changed,
DECISIONS.md additions, errors that remain.
```

## Template — `callvault-doc-writer`

```
Update CallVault docs for <event — feature shipped / sprint closed / release>.
Project root: "/home/primathon/Documents/p_projet/a_APP/4. callVault/".

Read CLAUDE.md. Read CHANGELOG.md and DECISIONS.md tail to avoid duplicates.
Verify the feature exists with grep before documenting it.

Files to update:
- CHANGELOG.md: <which section>
- DECISIONS.md: <new entry>
- README.md: <if status / tech stack changed>
- app/src/main/assets/docs/<NN-slug.md>: <if user-visible>

Voice: professional, neutral, terse. Final-quality English. Cite spec
sections (§3.<NN>) where relevant. No "we're excited", no exclamation marks.

Report (≤ 150 words): files updated, lines added/removed, anything you couldn't
verify in code (flag it).
```

## Template — `callvault-spec-reviewer`

```
Audit CallVault for production readiness. Project root:
"/home/primathon/Documents/p_projet/a_APP/4. callVault/".

Spec: /home/primathon/Downloads/callvault_mega_prompt.md (locked).

Read CLAUDE.md, then audit every spec section §3.1–§3.25 plus the §0 quality
bar. Cross-check DECISIONS.md so intentional deferrals don't get re-flagged.

Output a markdown report:
- Summary: shipped / partial / missing / deferred counts
- Ranked punch-list: P0 (blockers) → P1 (functional misses) → P2 (quality bar)
  → P3 (nice-to-haves)
- Per-section status table

Read-only. Do NOT modify any file.

Save the report to audit-<YYYY-MM-DD>.md in the project root.
```

## Composing multi-agent workflows

For a typical "add a feature" cycle:

```
1. callvault-android-engineer  → scaffolds files, wires the graph
2. callvault-ui-builder        → polishes the UI / states / accessibility
3. callvault-test-writer       → unit + DAO coverage
4. callvault-build-fixer       → user runs ./gradlew, agent fixes errors
5. callvault-doc-writer        → CHANGELOG / DECISIONS / in-app doc
```

Run them sequentially. Don't run the engineer + UI builder in parallel — they'll touch overlapping files.

## When to NOT spawn an agent

- Single-file Read or single Grep — do it yourself in the main session.
- Reading docs already in `.claude/docs/` — do it yourself.
- Cleanup / file rename / one-line fix — do it yourself.

The cost of spawning is the warmup + context load. For < 5 file ops, in-line is faster.
