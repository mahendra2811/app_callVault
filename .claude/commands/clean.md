---
description: Clean Gradle build artifacts (does NOT run gradle clean — just removes the build/ folders).
---

Clean stale build artifacts without invoking Gradle.

Confirm with the user first:

> "Remove `app/build/` and `.gradle/` cache directories? This forces the next build to start fresh."

On confirmation:

```bash
cd "/home/primathon/Documents/p_projet/a_APP/4. callNest"
rm -rf app/build .gradle/configuration-cache .gradle/caches/transforms-* 2>/dev/null
```

Don't `rm -rf .gradle` entirely — that nukes the dep cache and forces a 500 MB re-download.

After: surface what was removed and remind the user the next `/build` will be slower than usual.
