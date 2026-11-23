# Flixvora — Android app (Kotlin/Compose/Material3/Media3), package `com.flixvora.app`

## Hard constraints
- **No GitHub** anywhere: no remotes, no github.com/githubusercontent/jsdelivr URLs in source, gradle or dex. Verify with a dex scan after every build.
- Self-hosted static servers on ports **12000** (primary) / **12001** (mirror) serving `/workspace/project/dist`.
- Public base: `https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/` (mirror `work-2-...`).
- Release APK must be signed with `/workspace/project/release-key.jks` (config in `key.properties`).

## Release channel
`dist/flixvora/` holds `Flixvora.apk` + `version.json` (in-app update manifest). The APK's `UPDATE_MANIFEST_URL` defaults to this channel, so publishing = copy APK, update `versionName`/`versionCode`/`sha256`/`size` in `version.json`.

## Build
```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :app:assembleRelease        # needs chmod +x gradlew after a container reset
./gradlew :app:testDebugUnitTest      # 5 ApiParsingTest cases over captured live fixtures
```
The JDK is NOT preinstalled after a container reset: `sudo apt-get update && sudo apt-get install -y openjdk-21-jdk-headless`.

## Live TV
- Catalogue: official MovieBox TV endpoints reached through the **on-device** proxy `LocalMovieBoxServer` (`/api-proxy/wefeed-mobile-bff/live/...`). No external proxy, no playlists.
- Endpoints: `tv-country-list`, `tv-channel-list-by-country`, `tv-channel`.
- The endpoint caps each page at **10 items** regardless of `pageSize` and reports the real total in `pager.totalCount`; pagination must follow `hasMore`/`totalCount` (see `MovieRepository.getAllTvChannels`).
- `pager.page` / `nextPage` are JSON **strings**, counts are numbers. Typing page as Int makes Moshi reject the whole body.
- `channelId` is a string.
- Channels without an inline stream are still listed; the player resolves them by id on tap.

## Keystore
`/workspace/project/keys_private/` (chmod 700) holds a backup copy of `release-key.jks` + `key.properties` + fingerprints. It must never be inside `dist/` — that directory is served publicly.

## Stream recovery (`resource/v2`, `resource-position/v2`, `signCookie`)
`LocalMovieBoxServer` runs two recovery passes over the `play-info` streams before answering, both ported from the reference backend in `moviebox-internal-api-main`:

1. **`/subject-api/resource/v2`** is fetched with `isVip=true` and its `list` appended as extra mirrors. The endpoint gates the list behind a member check; the flag makes it return the same list a member would get.
2. **Decoy entries** (see `StreamUrlPolicy.isDecoyUrl`) are replaced in two steps: first by rebuilding the DASH manifest from the signed CloudFront policy in `signCookie` (`StreamUrlPolicy.authorizedManifestUrl`), then by asking `/subject-api/resource-position/v2`. A recovery result that is itself a decoy is discarded and the original entry is kept, so a failed recovery never invents a stream.

`StreamUrlPolicy` is deliberately context-free so its rules are unit tested (`StreamUrlPolicyTest`, 14 cases) against captured response shapes. `authorizedManifestUrl` only trusts a policy that is a **single** statement scoped to **one** wildcard under `sbcdn5.hakunaymatata.com`, on https/443, with no credentials, query or fragment, and whose `/dash/{subjectId}_{season}_{episode}_*` prefix matches the request — a policy issued for another title or episode is rejected rather than replayed.

## Response cache
`LocalMovieBoxServer` caches GET bodies for 5 minutes in memory and on disk. Only bodies that pass `StreamUrlPolicy.isCacheableBody` are stored — anything carrying an error code (`406 find no content`, `400 PARAMS_ERROR`, `401`) is skipped. Before 9.7 the cache accepted any non-empty body, so one upstream hiccup pinned the error on screen for the full window even after the API recovered. Do not loosen this check.

## Playback (play-info addressing)
- `play-info` addresses **movies** with `se=0&ep=0` and **series** with the real season/episode. The wrong pair returns `data.streams: []` with HTTP 200 — a silent empty, not an error.
- A subject's `isTvShow` flag comes from `subjectType == 2` and is right for ~11 of 12 catalogue rows, so `MovieRepository.getPlayInfo` retries the other addressing once for season 1 / episode 1 only. Deeper seasons legitimately have no stream and must not be re-asked.
- Guest tokens get a **23-second decoy trailer** (`macdn.aoneroom.com`, ~824 KB) for VIP-gated content, and every decoy is the *same* file. `signCookie` is empty for guests, so the CloudFront signed policy that the modded build reads to rebuild `sbcdn5.hakunaymatata.com/dash/...` URLs is simply not issued. Real streams need a member account; there is no client-side bypass to implement.
- `LocalMovieBoxServer` already treats `macdn.aoneroom.com` as a dummy host and prefers a non-dummy stream when one exists. Do not "fix" that filter — the host does serve real bytes, but only the decoy ones for a guest.

## Serving dist/ (survives container restarts)

`dist/` is published on :12000 and :12001 by `dist/serve.sh`, which is idempotent — it starts
only ports that are not already listening, so calling it repeatedly is safe. `~/.bashrc` calls
it on every new shell, because the container is restarted between sessions and a bare
`setsid` process does not survive that. If a download link 404s or times out, run
`sh /workspace/project/dist/serve.sh` first and re-check before assuming the file is gone.

## Source archive (`dist/flixvora/Flixvora-Full-Source-*.zip`)

Handing the code to the user means a zip served from the same public folder as the APK. Build it with a script, never by hand, because three things bite:

- **Never include `release-key.jks`, `key.properties`, `local.properties`, `keys_private/`, `keys_drive_build/`.** The zip is world-downloadable; the keystore is the update chain. Ship a `README.md` that tells the user to generate their own.
- **Strip build caches** (`.gradle/`, `build/`, `app/build/`, `.kotlin/`, `node_modules/`, `.next/`) and **the APKs inside `dist/`** — they double the size and are already downloadable.
- **Sanitize docs.** `RELEASE.md` and the vendored `moviebox-internal-api` READMEs carry repo URLs. Redact them; the user's constraint is no GitHub trace in anything they download.

Verify before publishing: `comm` the archive's file list against a `find` of the repo (expect zero missing), then sha256 every file (only the docs you edited should differ). A zip whose entry modes are right matters too — Python's `extractall()` drops the exec bit, so a `gradlew` that looks 755 inside the zip still needs `bash gradlew` when extracted that way. Test the archive by building from it in a scratch dir with a throwaway keystore.

## Decompiled reference APK (`reference/moviebox-app-decompiled/`)

The official MovieBox Pro APK (`com.community.oneroom`) is decompiled and shipped inside the
source archive, so the user can read the real app. Reproduce it with:

```bash
# jadx 1.5.1 + JDK 21; Gradle export is what makes Android Studio open it
java -Xmx8g -cp jadx/lib/jadx-1.5.1-all.jar jadx.cli.JadxCLI \
     -d /workspace/decompiled/moviebox-app --export-gradle --threads-count 4 MovieBox_Updated.apk
```

Expect `ERROR - finished with errors, count: 37` — that is normal for a 9-dex obfuscated APK,
not a failed run. Result: ~31.8k `.java`, 5.7k resources, `AndroidManifest.xml`, 58 `.so`.

Two jadx behaviours to know before you "fix" a perceived gap:

- **It inlines anonymous/inner classes** into the parent file. `a7.a` lives inside
  `com/cloud/hisavana/protocol/okhttptransport/a.java` as `new Runnable() { // from class: a7.a`.
  So a file count below the DEX class count is expected, not data loss.
- **It remaps packages on obfuscation collisions.** `androidx.lifecycle.*` is emitted under
  `androidx/view/`; the original name survives in Kotlin `@Metadata` and `renamed from:` comments.
  ~76 of 3,586 packages get moved.

The app's own code is `com/transsion/*` (4,763 files). Everything else is bundled SDKs.
jadx output does not compile — it is for reading only; keep building from `android-app/`.

## Gotchas
- Do not resurrect `/workspace/drive_build/AnyFilm-main`: it is the older v7.4 lineage (older UI, direct-signed TV calls that fail). `/workspace/project` is the current v9.6 lineage.
- `version.json` at the repo root is a stale artifact — the real manifest is `dist/flixvora/version.json`.
