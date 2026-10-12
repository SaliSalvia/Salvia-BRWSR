#!/usr/bin/env bash
# Browxer pre-flight — run before tagging a release (bash scripts/preflight.sh).
# Checks the 9 known CI/build risks in order of likelihood.
set -u
FAIL=0

step() { printf '\n[check] %s\n' "$1"; }
ok()   { printf '  ok: %s\n' "$1"; }
warn() { printf '  warn: %s\n' "$1"; }
fail() { printf '  FAIL: %s\n' "$1"; FAIL=1; }

step "1. youtubedl-android 0.15.x API call-sites"
if grep -rn --include='*.kt' 'YoutubeDLRequest(listOf' . ; then
  fail "YoutubeDLRequest must use the String constructor in 0.15.x"
else
  ok "no List-based YoutubeDLRequest constructor"
fi
if grep -rn --include='*.kt' 'getInfo(' engine app core feature ; then
  fail "VideoInfo/getInfo coupling found — use -J dump-json parsing instead"
else
  ok "no VideoInfo/getInfo coupling"
fi
if grep -rn --include='*.kt' -e 'FFmpeg.getInstance().execute("' . ; then
  fail "FFmpeg.execute must take List<String> in 0.15.x"
else
  ok "no String-based FFmpeg.execute"
fi
if grep -rn --include='*.kt' -e 'execute(req) {' . ; then
  fail "single-arg YoutubeDL.execute found — use execute(req, null as String?)"
else
  ok "no single-arg YoutubeDL.execute"
fi

step "2. Compose compiler options (Kotlin 2.x plugin era)"
if grep -rn 'composeOptions' --include='*.gradle.kts' . ; then
  fail "composeOptions must not appear — Kotlin 2.x uses the compose plugin"
else
  ok "no composeOptions in any build script"
fi
if grep -n 'composeCompiler' gradle/libs.versions.toml ; then
  fail "dead composeCompiler entry in version catalog"
else
  ok "no dead composeCompiler entry"
fi

step "3. gradle-wrapper.jar"
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  ok "wrapper jar is committed"
else
  warn "wrapper jar missing — CI bootstrap covers it, but commit it locally: gradle wrapper --gradle-version 8.9"
fi

step "4. Room schema directory"
if [ -d app/schemas ]; then
  ok "app/schemas exists"
else
  fail "app/schemas missing (room.schemaLocation)"
fi

step "5. Hilt graph (kspDebugKotlin)"
if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  if ./gradlew :app:kspDebugKotlin --stacktrace; then
    ok "Hilt graph clean"
  else
    fail "kspDebugKotlin failed — check duplicate bindings"
  fi
else
  warn "skipped (no runnable local wrapper) — CI will run it"
fi

step "6. Full debug build"
if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  if ./gradlew :app:assembleDebug --stacktrace; then
    ok "assembleDebug green"
  else
    fail "assembleDebug failed"
  fi
else
  warn "skipped (no runnable local wrapper) — CI will run it"
fi

step "7. Baseline profile"
if [ -f app/src/main/baseline-prof.txt ] && grep -Eq '^[HSP]*L' app/src/main/baseline-prof.txt; then
  if grep -qF '**' app/src/main/baseline-prof.txt; then
    fail "hand-written wildcard profile detected — generate a real one on a device (README-BUILD.md) or leave the stub empty"
  else
    ok "baseline-prof.txt contains real (device-generated) rules"
  fi
else
  warn "no active baseline profile — acceptable until a device-generated one lands"
fi

step "8. Bundled Vazirmatn fonts (GMS-free)"
N=$(ls core/ui/src/main/res/font/*.ttf 2>/dev/null | wc -l | tr -d ' ')
if [ "$N" -ge 5 ]; then
  ok "$N Vazirmatn TTFs bundled in core/ui/src/main/res/font"
else
  fail "only $N TTFs found — run: bash scripts/download-fonts.sh && git add core/ui/src/main/res/font/*.ttf"
fi
if grep -rq 'googlefonts' core/ui/src/main/java || grep -q 'ui-text-google-fonts' core/ui/build.gradle.kts gradle/libs.versions.toml; then
  fail "Google Fonts provider still wired — bundled TTFs only, no GMS dependency"
else
  ok "no GMS font provider"
fi

step "9. Macrobenchmark module compiles"
if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  if ./gradlew :macrobenchmark:assembleDebug --stacktrace; then
    ok "macrobenchmark module compiles"
  else
    fail ":macrobenchmark:assembleDebug failed"
  fi
else
  warn "skipped (no runnable local wrapper) — CI compiles it in the macrobenchmark-compile job"
fi

printf '\n'
if [ "$FAIL" -eq 0 ]; then
  printf 'PREFLIGHT PASSED — safe to tag v*\n'
  exit 0
else
  printf 'PREFLIGHT FAILED — fix the items above before tagging\n'
  exit 1
fi
