# Flixvora releases

## Version source of truth

The app version lives in `version.properties`:

```
versionName=11.5
versionCode=64
```

`app/build.gradle.kts` reads that file, so `BuildConfig.VERSION_NAME` (shown in
the **Me** tab) and the GitHub release tag always match. Do not edit the version
in two places.

To bump: change both values in `version.properties`, commit, and push to `main`.

## GitHub Actions APK build

Workflow: `.github/workflows/build.yml`

- Runs on every push to `main`, and manually from **Actions → Build Release APK → Run workflow**.
- Reads `version.properties` (manual inputs can override name/code for a one-off build).
- Builds a signed `app-release.apk` with `:app:assembleRelease`.
- Uploads it as a build artifact.
- Creates or updates the GitHub release `v<versionName>` with:
  - `app-release.apk`
  - `version.json` (version, SHA-256, size, download URL)

### Required repository secrets

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | base64 of the release keystore |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_PASSWORD` | key password |
| `KEY_ALIAS` | key alias |

The keystore and `key.properties` must never be committed.

## In-app update check

The Me tab calls the GitHub Releases API
(`BuildConfig.UPDATE_MANIFEST_URL`) and compares the latest tag with the
installed `VERSION_NAME`. It shows the real current version, the latest release,
and opens the APK asset when an update exists.

## Local build

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.
