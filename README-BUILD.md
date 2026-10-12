# 🏗️ Browxer — Build, Sign & Release Pipeline

## Prerequisites

- **JDK 17** (Temurin recommended)
- **Android Studio Ladybug+** with SDK 35
- **NDK 26.1.10909125** (bundled Python + FFmpeg + aria2c of youtubedl-android need it)
- Gradle 8.9 (wrapper properties pinned; see note on `gradle-wrapper.jar` below)

## Local build

```bash
# Debug (no signing needed)
./gradlew :app:assembleDebug

# Release (unsigned unless signing env vars are set)
./gradlew :app:assembleRelease
```

Signing is **conditional**: without the env vars the release build succeeds but stays unsigned
(so local builds never break). Set these env vars to sign locally:

```bash
export BROWXER_KEYSTORE_PATH=/abs/path/browxer-release.keystore
export BROWXER_KEYSTORE_PASSWORD=<store-pass>
export BROWXER_KEY_ALIAS=browxer
export BROWXER_KEY_PASSWORD=<key-pass>
```

## ⚠️ gradle-wrapper.jar

The Gradle **wrapper jar is a binary** and is not committed via the automation that created this repo.
Before your first local build, generate it (requires any local Gradle ≥ 8.5):

```bash
gradle wrapper --gradle-version 8.9
```

This creates `gradle/wrapper/gradle-wrapper.jar` and the pinned 8.9 distribution. Commit the jar
(it is exempted in `.gitignore`). The CI workflow bootstraps the wrapper automatically with
`gradle wrapper --gradle-version 8.9`, so CI works without it.

## Fonts (bundled Vazirmatn — no GMS dependency)

Persian + Latin UI text renders with **Vazirmatn TTFs bundled directly in the APK**
(`core/ui/src/main/res/font/`). There is deliberately **no** Google Fonts downloadable-fonts
provider: Browxer must render identically on devices without Google Play Services
(Huawei, Chinese ROMs, custom ROMs) and fully offline. Fetch the TTFs once and commit:

```bash
bash scripts/download-fonts.sh
git add core/ui/src/main/res/font/*.ttf
git commit -m "feat(ui): bundle Vazirmatn v33.003 TTFs"
```

While the TTFs are not committed yet, `:app:assembleDebug` fails in `core:ui` —
`scripts/preflight.sh` (check 8) tells you the same.

## Signing keystore (one-time setup)

```bash
keytool -genkeypair -v -keystore browxer-release.keystore \
  -alias browxer -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASS> -keypass <KEY_PASS> \
  -dname "CN=Browxer, OU=Dev, O=Browxer, L=, S=, C=IR"

base64 -w0 browxer-release.keystore > keystore.b64
```

### GitHub Secrets (Settings → Secrets and variables → Actions)

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Contents of `keystore.b64` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (`browxer`) |
| `KEY_PASSWORD` | Key password |

## CI/CD (GitHub Actions)

`.github/workflows/android-build.yml`:

- **Every push to `main`** → build job: debug + release APKs → upload as workflow artifacts (30-day retention).
  A separate **macrobenchmark-compile** job compiles `:macrobenchmark` only.
- **Every tag `v*`** → same build → **GitHub Release** created automatically with the signed APKs attached and auto-generated release notes.
- A red **macrobenchmark-compile** never blocks the APK release by design — but treat it as a real
  blocker anyway: a benchmark-module compile break usually means a public API in `:app` changed.

```bash
git tag v1.0.0
git push origin v1.0.0
# → Release appears at github.com/SaliSalvia/Salvia-BRWSR/releases
```

The runner installs JDK 17, Python 3.11 (yt-dlp), Android SDK 35, NDK 26.1.10909125, CMake 3.22.1,
decodes the keystore from `KEYSTORE_BASE64`, builds with R8 + resource shrink, and splits APKs by ABI.

## Performance & benchmarks

Budgets (design spec): **cold start ≤ 1200 ms**, **frame P99 ≤ 16 ms**.

CI **compiles** `:macrobenchmark` but cannot **run** it — GitHub-hosted runners have no
physical device, and emulator numbers are not representative. Measure on a real device
(or Firebase Test Lab):

```bash
# real device (representative numbers):
./gradlew :macrobenchmark:connectedDebugAndroidTest

# emulator only — NOT representative, needs the suppression flag:
./gradlew :macrobenchmark:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

`StartupBenchmark` (cold/warm, StartupTimingMetric) and `ScrollBenchmark` (FrameTimingMetric)
print the numbers in the test report.

### Real baseline profile (when you have a device)

The committed `app/src/main/baseline-prof.txt` is **intentionally a stub** — a wrong hand-written
profile makes startup *slower* (ART precompiles classes that are not actually hot). Generate a real one:

```bash
./gradlew :macrobenchmark:connectedDebugAndroidTest
cp macrobenchmark/build/outputs/connected_android_test_additional_output/*/*.txt \
   app/src/main/baseline-prof.txt
# then re-add androidx.profileinstaller to :app and commit
```

```scripts/preflight.sh` (check 7) accepts only device-generated profiles: any wildcard
(`**`) rule in the file fails the pre-flight.

## Cache layers & the frame watchdog (honest notes)

- The 50 MB **OkHttp cache** serves the download engine (resolvers, HLS/DASH parsers, parallel
  segment downloads). It does **not** make browser pages load faster: the WebView uses Chromium's
  own HTTP stack, whose cache mode is `LOAD_DEFAULT` and whose size is managed by the OS —
  not configurable. Do not expect page-load wins from the OkHttp cache.
- **JankReporter** (frame watchdog) is dev-only: attached inside `if (BuildConfig.DEBUG)` in
  `MainActivity`, never in release builds.

## APK output locations

ABI splits keep each APK under Play's 150 MB limit:

```
app/build/outputs/apk/release/
├── app-arm64-v8a-release.apk      # modern physical devices
├── app-armeabi-v7a-release.apk    # older 32-bit devices
├── app-x86_64-release.apk         # emulators
└── app-universal-release.apk      # all ABIs (largest)
```

Debug builds: `app/build/outputs/apk/debug/app-debug.apk` (applicationId suffix `.debug`).

## Known limitations (honest, tracked)

- **DASH SegmentTemplate**: representation-level URLs are used; full `<SegmentTemplate>`/`<SegmentURL>` URL templating is the next engine task.
- **APK size**: expect ~80–120 MB per ABI — youtubedl-android bundles Python 3.8, FFmpeg, and aria2c native libraries. `extractNativeLibs="true"` + `jniLibs.useLegacyPackaging = true` are required for yt-dlp's Python runtime.
- `READ_MEDIA_VIDEO/IMAGES/AUDIO` permissions must be granted on Android 13+ for in-app playback of MediaStore files.

## Acceptance checks

- `bash scripts/preflight.sh` → all 9 checks green (fonts, profile, macrobenchmark included)
- `./gradlew :app:assembleDebug` → success
- `./gradlew :app:assembleRelease` with signing env → signed APKs
- Push to `main` → both workflow jobs green, artifacts present
- Push `v1.0.0` → Release with APK assets
- Installs and launches on an ARM64 device; FAB pulses on media pages; no `NetworkOnMainThreadException`; no Android 14+ FGS crash
