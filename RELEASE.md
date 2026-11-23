# Flixvora — Self-hosted release (no GitHub)

Is build me app **GitHub se bilkul disconnected** hai. Koi GitHub Actions, koi
release, koi jsDelivr/raw-GitHub updater — kuch nahi. Sab kuch local + aapke
apne server se chalta hai.

---

## 1. Build output (ye APK install karo)

| Cheez | Value |
|---|---|
| APK file | `dist/flixvora/Flixvora.apk` (build ke baad `app/build/outputs/apk/release/app-release.apk`) |
| Package name | `com.flixvora.app` |
| App name | Flixvora |
| Version | `9.1` (versionCode `40`) |
| Size | ~6.7 MB |
| SHA-256 (APK) | `47c83ec687f7177feafb8341d7a38c0359c5c57eb6e8e282ef746b3e977dec87` |
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 36 |

## 2. Naya keystore + fingerprint (yahi par bana hai)

Keystore: `release-key.jks` (project root, gitignored)
Passwords: `key.properties` (project root, gitignored)

| Field | Value |
|---|---|
| Alias | `flixvora` |
| Key algorithm | RSA 4096 |
| Validity | ~30 saal (10950 din) |
| Cert DN | `CN=Flixvora, OU=Mobile, O=Flixvora, L=Mumbai, ST=Maharashtra, C=IN` |
| **SHA-1** | `93:DF:2C:FE:96:32:88:20:29:03:B9:0F:6B:66:CC:2E:03:82:CC:34` |
| **SHA-256** | `AD:8C:27:C1:A5:11:2D:AF:F5:D2:90:F3:55:5E:4D:07:85:AD:FD:59:1E:10:B8:4B:8A:CC:0B:B4:D9:2C:3C:CA` |
| MD5 | `ED:88:B9:41:FA:3A:B5:45:B9:66:92:70:0B:D2:32:7B` |

> **Zaroori:** `release-key.jks` + `key.properties` ko safe rakho aur backup lo.
> Inhe kho diya to future updates sign nahi kar paoge (same key ke bina in-place
> update install nahi hota). Ye dono GitHub par kabhi mat daalna.

## 3. GitHub se kya-kya hata diya

- `.github/workflows/android-release.yml` — **delete** kar diya (CI release pipeline hata).
- In-app updater pehle 2 GitHub URL use karta tha (base64 me chhupe hue):
  - `<redacted-cdn-url>
  - `<redacted-raw-url>

  Ab ye dono hata diye. Naya updater ek hi endpoint use karta hai jo aapke apne
  server se aata hai, aur build-time par badla ja sakta hai:

  `BuildConfig.UPDATE_MANIFEST_URL` → default
  `https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/version.json`

- Purana `UPDATE_REPO` (GitHub repo naam) BuildConfig field bhi hata diya.

Note: app ke **Live TV** feature me kuch third-party GitHub playlist URLs
(`sportlive18`, `iptv-org`, etc.) hai — ye content sources hai, aapke account se
koi lena-dena nahi. Chaho to inhe baad me badal sakte ho.

## 4. Future me update kaise nikalna hai

Update system pehle jaisa hi kaam karta hai (Settings → Check for Updates),
bas ab GitHub ki jagah aapka server:

1. `app/build.gradle.kts` me `versionCode` (23 → 24) aur `versionName` ("7.4" → "7.5") badhao.
2. `./publish_update.sh` chalao. Ye:
   - release APK build karega (aapke keystore se signed),
   - `dist/flixvora/Flixvora.apk` me copy karega,
   - `dist/flixvora/version.json` me naya versionCode + sahi `sha256` likh dega.
3. Update server chalte rehna chahiye (neeche dekho). App 24 ghante ke andar ya
   "Check for Updates" dabate hi naya version dikha dega.

App koi bhi APK install karne se pehle check karta hai: package name +
signing certificate match + `sha256`. Isliye **hamesha same `release-key.jks` se
sign karo**.

## 5. Update server

Local folder `dist/` serve hota hai (koi GitHub nahi):

```bash
# server start (already chal raha hai)
/workspace/serve_updates.sh            # python3 -m http.server 12000
# endpoint:
#   https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/version.json
#   https://work-1-vjjhalkhxmwfcrus.prod-runtime.all-hands.dev/flixvora/Flixvora.apk
```

**Zaroori baat:** ye `work-1-...prod-runtime.all-hands.dev` host is workspace
runtime ka hai — permanent nahi. Jab aapke paas apna permanent HTTPS host ho
(apna VPS/domain, ya koi bhi static hosting), sirf URL badal ke dobara build karo:

```bash
UPDATE_URL="https://apna-domain.com/flixvora/version.json" ./publish_update.sh
```

`dist/flixvora/` ki dono files (`Flixvora.apk` + `version.json`) apne host par
wahi path structure me daal do. Code me koi change nahi chahiye.

## 6. Build kaise hota hai (repeatable)

```bash
cd /workspace/project
./build_release.sh          # signed release APK
# ya
UPDATE_URL="https://..." ./publish_update.sh   # build + manifest update
```

Toolchain (is workspace me install kiya gaya):

- JDK 21 → `/usr/lib/jvm/java-21-openjdk-amd64`
- Android SDK → `/workspace/android-sdk` (platform 36.1, build-tools 36.1.0)
- Gradle 9.3.1 (wrapper), AGP 9.1.1, Kotlin 2.2.10

Sab dependencies Google Maven + Maven Central se aati hai — GitHub se kuch nahi.

## 7. APK install karne ka tarika

1. `dist/flixvora/Flixvora.apk` phone me bhejo (USB / direct download).
2. Purani Flixvora install ho to **pehle uninstall** karo (naya signing key hai,
   isliye in-place update nahi hoga — ek baar uninstall zaroori).
3. APK kholo → "Install unknown apps" allow karo → Install.
