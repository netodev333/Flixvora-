#!/usr/bin/env bash
# Builds the downloadable source archive: this repo + the MovieBox API reference.
# The zip lands in dist/ so it is served next to the APK.
set -euo pipefail

ROOT=/workspace/project
REF_API=/workspace/drive_dl/extracted/All-in-one-main/moviebox-internal-api-main
REF_V74=/workspace/drive_build/AnyFilm-main
REF_MBOX=/workspace/decompiled/moviebox-app   # jadx --export-gradle output of the official APK
VER=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$ROOT/app/build.gradle.kts" | head -1)
NAME=Flixvora-Full-Source
STAGE=$(mktemp -d)/$NAME
OUT=$ROOT/dist/flixvora/$NAME-v$VER.zip

mkdir -p "$STAGE"/android-app "$STAGE"/reference/anyfilm-v7.4 \
         "$STAGE"/reference/moviebox-backend "$STAGE"/reference/moviebox-app-decompiled
copy() {
  tar -cf - -C "$1" --exclude=.gradle --exclude=.kotlin --exclude=build \
    --exclude=app/build --exclude=.git --exclude=node_modules --exclude=.next \
    --exclude=__pycache__ --exclude='*.jks' --exclude='*.keystore' \
    --exclude=key.properties --exclude=local.properties \
    --exclude=keys_private --exclude=keys_drive_build --exclude='*.apk' \
    --exclude='Flixvora-Full-Source-*.zip' \
    . | tar -xf - -C "$2"
}
copy "$ROOT" "$STAGE/android-app"
copy "$REF_V74" "$STAGE/reference/anyfilm-v7.4"
copy "$REF_API" "$STAGE/reference/moviebox-backend"
copy "$REF_MBOX" "$STAGE/reference/moviebox-app-decompiled"
cp "$ROOT/tools/source-archive/README.md" "$STAGE/README.md"
cp "$ROOT/tools/source-archive/README-moviebox-decompiled.md" \
   "$STAGE/reference/moviebox-app-decompiled/README.md"
rm -f "$STAGE/android-app/version.json"   # stale root manifest; dist/ one is live

python3 - "$STAGE" "$OUT" <<'PY'
import os, re, sys, zipfile
stage, out = sys.argv[1], sys.argv[2]
patterns = [
    (r'https?://cdn\.jsdelivr\.net/gh/\S+', '<redacted-cdn-url>'),
    (r'https?://raw\.githubusercontent\.com/\S+', '<redacted-raw-url>'),
    (r'https?://github\.com/(?:solo12345689|mrsameer6910-stack)/\S+', '<redacted-repo-url>'),
]
for dp, _, fns in os.walk(stage):
    for f in fns:
        if not f.endswith(('.md', '.txt')):
            continue
        p = os.path.join(dp, f)
        s = orig = open(p, encoding='utf-8', errors='ignore').read()
        for pat, rep in patterns:
            s = re.sub(pat, rep, s)
        if s != orig:
            open(p, 'w', encoding='utf-8').write(s)

root = os.path.basename(stage)
n = 0
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for dp, dns, fns in os.walk(stage):
        dns[:] = [d for d in dns if d not in {'.gradle', '.kotlin', 'build', 'node_modules', '.next', '__pycache__'}]
        for f in fns:
            full = os.path.join(dp, f)
            arc = os.path.join(root, os.path.relpath(full, stage))
            z.write(full, arc)
            n += 1
print(f'{n} files -> {out} ({os.path.getsize(out):,} bytes)')

bad = [x for x in zipfile.ZipFile(out).namelist()
       if x.endswith(('.jks', '.keystore', '.apk')) or 'key.properties' in x or x.endswith('local.properties')]
if bad:
    sys.exit(f'REFUSING: secrets in archive: {bad}')
PY
