# Flixvora — Full Source (v9.7)

Ye poora source code hai: Flixvora Android app + MovieBox ka reverse-engineered
backend reference, dono ek hi jagah. Isse aap APK khud build kar sakte ho.

```
Flixvora-Full-Source/
├── android-app/                    ← Ye chalne wala app hai (Kotlin/Compose). Yahi se APK banta hai.
└── reference/                      ← Padhne ke liye reference code, build me include nahi hota
    ├── anyfilm-v7.4/               ← Purana v7.4 Android lineage (v9.7 se pehle ka)
    ├── moviebox-backend/           ← MovieBox API ka Python reference client + dashboard
    └── moviebox-app-decompiled/    ← Official MovieBox Pro APK ka 100% decompiled source
```

## Build kaise kare

Zaroorat: JDK 21, Android SDK (compileSdk 36).

```bash
cd android-app
./gradlew assembleRelease        # signed APK -> app/build/outputs/apk/release/
./gradlew testDebugUnitTest      # 22 unit tests
```

## Signing keystore

Archive me keystore **nahi** hai — wo private key hai aur public download me
nahi jaani chahiye. Apna keystore banao ya purana rakho:

```bash
keytool -genkeypair -v -keystore release-key.jks -alias flixvora \
        -keyalg RSA -keysize 4096 -validity 10000
```

Phir `android-app/key.properties` banao:

```properties
storeFile=release-key.jks
storePassword=<apna password>
keyAlias=flixvora
keyPassword=<apna password>
```

> **Zaroori:** agar aap users ko update dena chahte ho to wahi keystore use karo
> jo pehle APK sign karne me laga tha. Alag key se in-place update install nahi
> hota — user ko uninstall karna padta hai.

## Update manifest

App khud ko update karne ke liye ek `version.json` fetch karta hai. Wo
`dist/flixvora/version.json` me hai. `downloadUrl` ko apne server ka address
dena hoga — build time par override kar sakte ho:

```bash
./gradlew assembleRelease -PupdateManifestUrl=https://aapka-server/version.json
```

## Kya-kya andar hai

**android-app/** — 44 Kotlin files, ~26k lines. Khaas cheezein:

- `data/network/LocalMovieBoxServer.kt` — on-device HTTP proxy jo MovieBox API
  ko sign karta hai. Stream recovery (`resource/v2`, `resource-position/v2`),
  decoy filtering, response cache — sab yahin hai.
- `data/network/StreamUrlPolicy.kt` — pure rules: CloudFront signed policy se
  manifest banana, decoy detect karna, error body cache na karna. Iske 14 tests
  `app/src/test/` me hain.
- `ui/screens/` — Compose screens (Home, Search, Detail, Player, LiveTV, ...).
- `AGENTS.md` — repo ka technical notebook: build commands, playback quirks,
  aur wo galtiyan jo dobara nahi karni.

**reference/moviebox-backend/** — MovieBox API ka Python client (`moviebox_api/`)
aur ek Next.js dashboard. Signing scheme, endpoint list, aur
`OFFICIAL_API_DOCUMENTATION.md` yahan se aaya. Ye sirf reference hai; APK me
kuch bhi isse compile nahi hota.

**reference/moviebox-app-decompiled/** — Official MovieBox Pro APK
(`com.community.oneroom`, v3.0.14.0415.03) ka poora jadx decompilation:
**31,847 Java files**, decoded `AndroidManifest.xml`, 5,697 resources, assets,
aur 58 native `.so` (arm64-v8a + armeabi-v7a). Ye bhi sirf padhne ke liye hai —
jadx output compile nahi hota. Asli app logic `com/transsion/*` me hai (4,763
files); baaki `com/google`, `com/facebook`, `com/bytedance` waghera bundled
third-party SDKs hain. Details uske apne `README.md` me hain.

## Third-party credit

App AnyFilm (MIT License, © solo12345689) se nikla hai. LICENSE file dekho —
MIT kehta hai ki copyright notice copies me rakhna zaroori hai, isliye wo rakha gaya hai.

## Note

MovieBox ke content endpoints (`subject-api/get`, `play-info`, `resource/v2`)
is waqt server side se `406 find no content` de rahe hain. Ye app ka bug nahi
hai. `tab-operating`, `season-info`, `search-rank` abhi bhi 200 dete hain, to
catalogue browsing chalta rehta hai — playback tab tak band rahega jab tak
upstream theek nahi hota. v9.7 ka cache fix isliye zaroori tha: pehle ek error
5 minute tak cache ho jata tha, ab nahi hota, to upstream theek hote hi app
turant naya data le lega.
