---
description: Run unit tests via `./gradlew test` (with confirmation). For instrumentation tests, use `/test-instrumented`.
---

Run JVM unit tests for CallVault.

Steps:

1. Confirm before running — pulls test deps if not cached:
   > "Run `./gradlew test`? Pulls test deps on first run."

2. On confirmation:
   ```bash
   cd "/home/primathon/Documents/p_projet/a_APP/4. callVault"
   ./gradlew --offline test 2>&1 | tee /tmp/cv-test.log
   ```
   Retry without `--offline` if cache miss.

3. Summarize results:
   ```bash
   tail -30 /tmp/cv-test.log
   ```
   Look for `BUILD SUCCESSFUL` / `BUILD FAILED`. Count passed/failed:
   ```bash
   grep -E "tests completed|tests failed" /tmp/cv-test.log
   ```

4. If failed, parse the failures and route to the `callvault-build-fixer` or `callvault-test-writer` agent depending on whether the failure is compile-time or assertion.

5. HTML report path on success:
   `app/build/reports/tests/testDebugUnitTest/index.html`
