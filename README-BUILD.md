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

- **Every push to `main`** → build debug + release APKs → upload as workflow artifacts (30-day retention).
- **Every tag `v*`** → same build → **GitHub Release** created automatically with the signed APKs attached and auto-generated release notes.

```bash
git tag v1.0.0
git push origin v1.0.0
# → Release appears at github.com/SaliSalvia/Salvia-BRWSR/releases
```

The runner installs JDK 17, Python 3.11 (yt-dlp), Android SDK 35, NDK 26.1.10909125, CMake 3.22.1,
decodes the keystore from `KEYSTORE_BASE64`, builds with R8 + resource shrink, and splits APKs by ABI.

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
- **HLS downloads are sequential** per playlist; a concurrent segment pool is pending.
- **APK size**: expect ~80–120 MB per ABI — youtubedl-android bundles Python 3.8, FFmpeg, and aria2c native libraries. `extractNativeLibs="true"` + `jniLibs.useLegacyPackaging = true` are required for yt-dlp's Python runtime.
- `READ_MEDIA_VIDEO/IMAGES/AUDIO` permissions must be granted on Android 13+ for in-app playback of MediaStore files.

## Acceptance checks

- `./gradlew :app:assembleDebug` → success
- `./gradlew :app:assembleRelease` with signing env → signed APKs
- Push to `main` → workflow green, artifacts present
- Push `v1.0.0` → Release with APK assets
- Installs and launches on an ARM64 device; FAB pulses on media pages; no `NetworkOnMainThreadException`; no Android 14+ FGS crash
