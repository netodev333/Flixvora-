# MovieBox Pro — decompiled source (`com.community.oneroom`)

Full static decompilation of the official MovieBox Pro Android APK, produced with
**jadx 1.5.1** (`--export-gradle`). Nothing here was executed — the APK was only unpacked
and disassembled.

## What this is

| | |
|---|---|
| Source APK | `MovieBox_Updated.apk` from `movieboxpro.pro` |
| APK size / SHA-256 | 126,328,263 bytes / `c78ab45289ffddad…` |
| Package | `com.community.oneroom` |
| versionName / versionCode | `3.0.14.0415.03` / `50020096` |
| minSdk / targetSdk / compileSdk | 23 / 35 / 35 |
| DEX files | 9 (`classes.dex` … `classes9.dex`) |
| Java files recovered | **31,847** |
| Resources recovered | **5,697** |
| Native ABIs | `arm64-v8a`, `armeabi-v7a` (58 `.so`) |
| Decompile errors | 37 methods out of millions (jadx could not resolve; normal for a 9-dex obfuscated APK) |

Verification done on this dump: 59,831 classes exist in the APK's 9 DEX files; 42,154 are
matched directly to a file or a `// from class:` marker. The remaining ~17.7k are inner and
anonymous classes that jadx **inlines** into their parent file (16,968 of them carry `$`), plus
76 packages that jadx **remaps** to avoid obfuscation collisions — e.g. `androidx.lifecycle.*`
is emitted under `androidx/view/`, with the original name preserved in Kotlin `@Metadata` and
`renamed from:` comments. Spot-checks (`a7.a`, `androidx.lifecycle.Lifecycle`) confirm the code
is present, just relocated. Nothing is silently dropped except the 37 unparseable methods.

Layout is jadx's Gradle export, so Android Studio can open the folder directly:

```
app/src/main/AndroidManifest.xml     decoded manifest
app/src/main/java/                   31,847 .java files
app/src/main/res/                    5,697 decoded resources
app/src/main/assets/                 app assets (see note on origin.apk)
app/src/main/lib/<abi>/*.so          native libraries
app/src/main/classes*.dex            original bytecode, kept for reference
```

## Where the real code lives

The app is heavily obfuscated — most classes sit in single/two-letter packages (`a/`, `a0/`, …).
The readable, non-obfuscated app code is here:

- `com/transsion/*` — 4,763 files. The core: `home`, `homeapi`, `moviedetail`,
  `moviedetailapi`, `player`, `room`, `usercenter`, `push`, `rewardscenter`,
  `commercialization`, `mb`, `postdetail`, `videofloat`, `fissionapi`, `libpreloader`.
- `com/transsnet/downloader` — the download engine (238 references).
- `com/community/*` — the `oneroom` shell that hosts the above.

Everything under `com/google`, `com/facebook`, `com/bytedance`, `com/alibaba`,
`com/tencent`, `com/vungle`, `com/mbridge`, `com/hisavana`, `androidx`, `io/*`, `net/*`
is bundled third-party SDK code (Firebase, Glide, Gson, ad networks, analytics), not
MovieBox's own logic.

`com/github/lzyzsd/jsbridge` is the JS bridge — a public open-source library that happens
to live under a `com/github` package. It is not a reference to any repository.

## Read this before you try to build it

**This tree does not compile as-is, and jadx output never will.** It is reference source:

- Obfuscated names are meaningless (`a.b.c()`), so it reads as structure, not prose.
- Decompiled generics, `synthetic` accessors and inlined lambdas are approximate; some
  methods carry jadx error comments.
- The Gradle files are jadx's stub (`com.android.tools.build:gradle:4.2.2`) with no
  dependencies declared — the real dependency set would have to be reconstructed.
- Resources are decoded but not re-encoded into a buildable `resources.arsc`.

Use it to read behaviour, endpoints, headers, crypto, and flow — then reimplement.
If you need a rebuildable tree, the practical route is: use this for reference, and keep
building the app from `android-app/` in this same archive.

## Notes

- `app/src/main/assets/SignatureKiller/origin.apk` (59 MB) is a nested APK shipped inside
  MovieBox's own assets — a signature-verification patching tool the app carries. **It is
  excluded from this archive** (the packer drops `*.apk`): it is a binary, not source, and it
  is the bulk of the folder's size. Pull it from the original APK if you want it:
  `unzip MovieBox_Updated.apk 'assets/SignatureKiller/origin.apk'`.
- `classes*.dex` are the original bytecode, kept so nothing is lost. `baksmali` can produce
  `smali/` from them if you prefer assembly-level reading over Java.
- The app's own API hosts, client IDs and signing material appear throughout this source,
  because they are compiled into the APK. They are MovieBox's credentials, not yours.
