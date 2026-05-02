#!/usr/bin/env bash
# Print a quick CallVault project stat snapshot. No build, no network.
# Usage: bash .claude/scripts/cv-stats.sh

set -euo pipefail

ROOT="/home/primathon/Documents/p_projet/a_APP/4. callVault"
cd "$ROOT"

echo "=== CallVault project stats ==="
echo

printf "Kotlin source files     : "
find app/src/main/java/com/callvault/app -name "*.kt" 2>/dev/null | wc -l

printf "  ui/ files             : "
find app/src/main/java/com/callvault/app/ui -name "*.kt" 2>/dev/null | wc -l

printf "  domain/ files         : "
find app/src/main/java/com/callvault/app/domain -name "*.kt" 2>/dev/null | wc -l

printf "  data/ files           : "
find app/src/main/java/com/callvault/app/data -name "*.kt" 2>/dev/null | wc -l

printf "Compose @Preview count  : "
grep -rln "@Preview" app/src/main/java/com/callvault/app 2>/dev/null | wc -l

printf "Composable count        : "
grep -r "@Composable" app/src/main/java/com/callvault/app 2>/dev/null | wc -l

printf "ViewModels              : "
find app/src/main/java/com/callvault/app -name "*ViewModel.kt" 2>/dev/null | wc -l

printf "DAOs                    : "
find app/src/main/java/com/callvault/app/data/local/dao -name "*.kt" 2>/dev/null | wc -l

printf "Use cases               : "
find app/src/main/java/com/callvault/app/domain/usecase -name "*.kt" 2>/dev/null | wc -l

printf "Workers                 : "
find app/src/main/java/com/callvault/app/data/work -name "*.kt" 2>/dev/null | wc -l

printf "Unit tests              : "
find app/src/test -name "*Test.kt" 2>/dev/null | wc -l

printf "Instrumentation tests   : "
find app/src/androidTest -name "*Test.kt" 2>/dev/null | wc -l

printf "In-app docs articles    : "
ls app/src/main/assets/docs/*.md 2>/dev/null | wc -l

printf "Strings                 : "
grep -c "<string " app/src/main/res/values/strings.xml 2>/dev/null || echo 0

printf "Permissions declared    : "
grep -c "<uses-permission" app/src/main/AndroidManifest.xml 2>/dev/null || echo 0

echo
echo "TODO status (P0–P3 unchecked counts):"
for tier in "P0" "P1" "P2" "P3"; do
  count=$(awk -v t="$tier" '
    $0 ~ "^## " t " " {flag=1; next}
    flag && /^## / {flag=0}
    flag && /^- \[ \]/ {n++}
    END {print n+0}
  ' TODO.md 2>/dev/null)
  printf "  %s : %s\n" "$tier" "$count"
done

echo
echo "Last 5 modified .kt files:"
find app/src/main/java/com/callvault/app -name "*.kt" -printf '%T@ %p\n' 2>/dev/null \
  | sort -nr | head -5 | awk '{print "  " $2}'
