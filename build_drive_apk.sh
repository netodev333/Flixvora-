#!/bin/sh
# Rebuilds and publishes the /flixvora-drive/ channel.
#
# Usage: ./build_drive_apk.sh [versionName] [versionCode]
#
# The APK must always be signed with keys_drive_build/release-key.jks. Signing
# with any other key makes Android reject the install over an existing copy
# (INSTALL_FAILED_UPDATE_INCOMPATIBLE), which is unrecoverable for the user
# short of uninstalling.
set -e

NAME=${1:-7.4}
CODE=${2:-23}
SRC=/workspace/drive_build/AnyFilm-main
DIST=/workspace/project/dist/flixvora-drive

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
export PATH="$JAVA_HOME/bin:$PATH"

cd "$SRC"
./gradlew :app:assembleRelease

APK="$SRC/app/build/outputs/apk/release/app-release.apk"
SHA=$(sha256sum "$APK" | cut -d' ' -f1)
SIZE=$(stat -c%s "$APK")

mkdir -p "$DIST"
cp "$APK" "$DIST/Flixvora.apk"

python3 - "$DIST" "$NAME" "$CODE" "$SHA" "$SIZE" <<'PY'
import json, sys
dist, name, code, sha, size = sys.argv[1:6]
p = f"{dist}/version.json"
d = json.load(open(p))
d.update(versionName=name, versionCode=int(code), sha256=sha, size=int(size))
json.dump(d, open(p, "w"), indent=2, ensure_ascii=False)
PY

echo "Published $NAME ($CODE) sha256=$SHA"
