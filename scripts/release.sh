#!/usr/bin/env bash
# Build a signed release APK for Call Nest.
#
# Usage:
#   ./scripts/release.sh                # build current version
#   ./scripts/release.sh --bump-patch   # 1.0.0 → 1.0.1, versionCode++
#   ./scripts/release.sh --bump-minor   # 1.0.0 → 1.1.0
#   ./scripts/release.sh --bump-major   # 1.0.0 → 2.0.0
#   ./scripts/release.sh --version 1.2.3   # set versionName explicitly (versionCode++ regardless)
#
# Outputs:
#   ~/Releases/CallNest-<versionName>.apk
#   ~/Releases/versions-stable.json   (for the in-app self-update mechanism)
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

OUTPUT_DIR="${HOME}/Releases"
GRADLE_FILE="app/build.gradle.kts"

if [ ! -f "keystore.properties" ]; then
  echo "🚨 keystore.properties missing. Create it (see PLAY_STORE_READINESS.md §1.1) then re-run." >&2
  exit 1
fi
if [ ! -f "$(grep '^storeFile' keystore.properties | cut -d= -f2 | tr -d ' ')" ]; then
  echo "🚨 keystore file referenced in keystore.properties not found." >&2
  exit 1
fi

# --- Read current version ---
CURRENT_CODE=$(grep -E '^\s*versionCode\s*=' "$GRADLE_FILE" | grep -oE '[0-9]+' | head -1)
CURRENT_NAME=$(grep -E '^\s*versionName\s*=' "$GRADLE_FILE" | sed -E 's/.*"([^"]+)".*/\1/')
echo "Current: versionCode=$CURRENT_CODE versionName=$CURRENT_NAME"

# --- Parse args ---
BUMP=""
EXPLICIT_NAME=""
while [ $# -gt 0 ]; do
  case "$1" in
    --bump-patch) BUMP="patch"; shift ;;
    --bump-minor) BUMP="minor"; shift ;;
    --bump-major) BUMP="major"; shift ;;
    --version)    EXPLICIT_NAME="$2"; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

NEW_NAME="$CURRENT_NAME"
NEW_CODE="$CURRENT_CODE"

if [ -n "$EXPLICIT_NAME" ]; then
  NEW_NAME="$EXPLICIT_NAME"
  NEW_CODE=$((CURRENT_CODE + 1))
elif [ -n "$BUMP" ]; then
  IFS='.' read -r MAJ MIN PAT <<< "$CURRENT_NAME"
  case "$BUMP" in
    patch) PAT=$((PAT + 1)) ;;
    minor) MIN=$((MIN + 1)); PAT=0 ;;
    major) MAJ=$((MAJ + 1)); MIN=0; PAT=0 ;;
  esac
  NEW_NAME="${MAJ}.${MIN}.${PAT}"
  NEW_CODE=$((CURRENT_CODE + 1))
fi

if [ "$NEW_CODE" != "$CURRENT_CODE" ] || [ "$NEW_NAME" != "$CURRENT_NAME" ]; then
  echo "→ bumping to versionCode=$NEW_CODE versionName=$NEW_NAME"
  sed -i -E "s/(versionCode\s*=\s*)[0-9]+/\1${NEW_CODE}/" "$GRADLE_FILE"
  sed -i -E "s/(versionName\s*=\s*)\"[^\"]+\"/\1\"${NEW_NAME}\"/" "$GRADLE_FILE"
fi

# --- Build ---
echo "→ ./gradlew clean assembleRelease (this takes ~2 min)"
./gradlew clean assembleRelease

APK_SRC="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_SRC" ]; then
  echo "🚨 release APK not produced. Build probably failed silently." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
APK_DST="${OUTPUT_DIR}/CallNest-${NEW_NAME}.apk"
cp "$APK_SRC" "$APK_DST"
SIZE=$(stat -c%s "$APK_DST")
SHA256=$(sha256sum "$APK_DST" | cut -d' ' -f1)

# --- versions-stable.json for in-app self-update (BuildConfig.UPDATE_MANIFEST_STABLE_URL) ---
RELEASE_NOTES_FILE="${PROJECT_ROOT}/CHANGELOG.md"
RELEASE_NOTES="See CHANGELOG.md"
if [ -f "$RELEASE_NOTES_FILE" ]; then
  RELEASE_NOTES=$(awk '/^## /{c++} c==1{print} c==2{exit}' "$RELEASE_NOTES_FILE" | head -20 | sed 's/"/\\"/g' | tr '\n' ' ')
fi

cat > "${OUTPUT_DIR}/versions-stable.json" <<EOF
{
  "versionCode": ${NEW_CODE},
  "versionName": "${NEW_NAME}",
  "url": "https://callnest.pooniya.com/apk/callnest-latest.apk",
  "size": ${SIZE},
  "sha256": "${SHA256}",
  "minSdk": 26,
  "minSupportedVersionCode": 1,
  "releaseNotes": "${RELEASE_NOTES}"
}
EOF

echo ""
echo "✅ done."
echo "  APK:        ${APK_DST}"
echo "  Size:       $((SIZE / 1024 / 1024)) MB"
echo "  SHA256:     ${SHA256}"
echo "  Manifest:   ${OUTPUT_DIR}/versions-stable.json"
echo ""
echo "Next:"
echo "  1. Test the APK on a phone:    adb install -r ${APK_DST}"
echo "  2. Upload APK + manifest to your hosting (GitHub Pages / your domain / Drive)."
echo "  3. WhatsApp the APK to first 5 testers, watch logcat for issues."
