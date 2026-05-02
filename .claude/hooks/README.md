# Hooks (optional)

Claude Code hooks for this project. Hooks are shell commands the Claude Code harness runs in response to events; they let you enforce policy without relying on Claude to remember.

This folder is documentation only — actual hook wiring lives in `.claude/settings.json` under a `hooks` key. Sample wiring is below.

## Suggested hooks

### Block direct edits to spec / locked files

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "bash -c 'jq -r .params.file_path | grep -q -E \"callvault_mega_prompt.md|gradle/libs.versions.toml\" && echo BLOCK || true'"
          }
        ]
      }
    ]
  }
}
```

The locked spec at `/home/primathon/Downloads/callvault_mega_prompt.md` and the Gradle version catalog should not be edited by Claude without an explicit user "yes". Adjust the regex to taste.

### Auto-format Kotlin on Write

If [`ktlint`](https://pinterest.github.io/ktlint/) is installed:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "bash -c 'jq -r .params.file_path | grep -E \"\\.kt$\" | xargs -r ktlint -F'"
          }
        ]
      }
    ]
  }
}
```

### Append to `TODO.md` when Claude says "stop"

```json
{
  "hooks": {
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "echo \"\\nStopped at $(date -Iseconds)\\n\" >> '/home/primathon/Documents/p_projet/a_APP/4. callVault/TODO.md'"
          }
        ]
      }
    ]
  }
}
```

Useful for picking work back up.

## Why hooks live in settings.json, not here

The Claude Code harness only reads hook configuration from `.claude/settings.json` (project) or `~/.claude/settings.json` (user). This folder is for documenting the hooks the team has agreed on — copy them into `settings.json` to activate.

## When to add a hook

- A class of mistake has happened twice. (Once is bad luck; twice is a rule.)
- A repeated manual step (formatter, lint preview) can be automated.
- A high-cost mistake (deleting the spec, force-pushing) can be cheaply prevented.

## When NOT to add a hook

- One-time annoyances. Live with it.
- Heavy lifting (running the full build). Use a slash command instead — they're explicit and reviewable.
- Anything that requires a network call. Hooks should be fast and local.
