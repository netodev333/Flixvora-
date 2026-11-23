#!/usr/bin/env bash
# Local Flixvora release build. No GitHub, no CI, no network upload.
# Signs with release-key.jks (alias flixvora) via key.properties.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

cd "$(dirname "$0")"

# The in-app updater polls this endpoint. Change it with UPDATE_URL=... ./build_release.sh
UPDATE_URL="${UPDATE_URL:-https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/version.json}"

./gradlew :app:assembleRelease -PupdateManifestUrl="$UPDATE_URL" "$@"

APK="app/build/outputs/apk/release/app-release.apk"
echo
echo "Built: $APK"
sha256sum "$APK"
