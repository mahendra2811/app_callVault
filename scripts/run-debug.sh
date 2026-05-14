#!/usr/bin/env bash
# Build, install and launch the debug variant — handles the applicationId vs
# namespace mismatch (applicationId="com.callvault.app", source package
# com.callNest.app) so you don't have to remember the right `adb am start`
# incantation.
#
# Usage:
#   ./scripts/run-debug.sh              # build → install → launch
#   ./scripts/run-debug.sh --no-build   # skip gradle, just install + launch
#   ./scripts/run-debug.sh --launch     # just launch (no build, no install)
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

GRADLE_FILE="app/build.gradle.kts"
MANIFEST="app/src/main/AndroidManifest.xml"
APK="app/build/outputs/apk/debug/app-debug.apk"

# Derive applicationId + .debug suffix straight from build.gradle.kts so this
# survives any future rename without us forgetting to update the script.
APP_ID=$(grep -E '^\s*applicationId\s*=' "$GRADLE_FILE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
SUFFIX=$(grep -E 'applicationIdSuffix\s*=' "$GRADLE_FILE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/' || echo "")
DEBUG_PKG="${APP_ID}${SUFFIX}"

# MainActivity lives in the namespace (source package), which differs from
# applicationId. Grab it from android.namespace = "...".
NAMESPACE=$(grep -E '^\s*namespace\s*=' "$GRADLE_FILE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
ACTIVITY="${NAMESPACE}.MainActivity"

MODE="all"
case "${1:-}" in
  --no-build) MODE="install-launch" ;;
  --launch)   MODE="launch" ;;
  "")         MODE="all" ;;
  *) echo "Unknown arg: $1"; exit 1 ;;
esac

if [ "$MODE" = "all" ]; then
  echo "→ ./gradlew assembleDebug"
  ./gradlew --no-daemon assembleDebug
fi

if [ "$MODE" = "all" ] || [ "$MODE" = "install-launch" ]; then
  if [ ! -f "$APK" ]; then
    echo "🚨 APK not found at $APK — run without --no-build." >&2
    exit 1
  fi
  echo "→ adb install -r $APK"
  adb install -r "$APK"
fi

echo "→ am start -n ${DEBUG_PKG}/${ACTIVITY}"
adb shell am start -n "${DEBUG_PKG}/${ACTIVITY}"
echo ""
echo "  package:  $DEBUG_PKG"
echo "  activity: $ACTIVITY"
