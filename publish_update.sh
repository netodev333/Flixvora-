#!/usr/bin/env bash
# Build a new release and publish it to the self-hosted update folder.
# Usage: ./publish_update.sh
#   1. Bump versionCode/versionName in app/build.gradle.kts
#   2. Run this script
#   3. Keep the update server running: /workspace/serve_updates.sh
set -euo pipefail

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

UPDATE_URL="${UPDATE_URL:-https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/version.json}"
UPDATE_BASE="${UPDATE_URL%/*}"   # strip /version.json

./build_release.sh

mkdir -p dist/flixvora
cp app/build/outputs/apk/release/app-release.apk dist/flixvora/Flixvora.apk
SHA=$(sha256sum dist/flixvora/Flixvora.apk | cut -d' ' -f1)

# Read versionCode/versionName straight from the built APK so the manifest can
# never drift from the artifact.
BADGING=$(aapt2 dump badging dist/flixvora/Flixvora.apk 2>/dev/null || "$ANDROID_HOME/build-tools/36.1.0/aapt2" dump badging dist/flixvora/Flixvora.apk)
VCODE=$(echo "$BADGING" | head -1 | sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p")
VNAME=$(echo "$BADGING" | head -1 | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")

cat > dist/flixvora/version.json <<EOF
{
  "versionName": "$VNAME",
  "versionCode": $VCODE,
  "forceUpdate": false,
  "changelog": "${CHANGELOG:-Flixvora v$VNAME}",
  "downloadUrl": "$UPDATE_BASE/Flixvora.apk",
  "sha256": "$SHA"
}
EOF

echo
echo "Published v$VNAME (code $VCODE)  sha256=$SHA"
echo "Manifest: dist/flixvora/version.json"
