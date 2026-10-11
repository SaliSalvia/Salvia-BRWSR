#!/usr/bin/env bash
# Browxer pre-flight — run before tagging a release (bash scripts/preflight.sh).
# Checks the 6 known CI risks in order of likelihood.
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

printf '\n'
if [ "$FAIL" -eq 0 ]; then
  printf 'PREFLIGHT PASSED — safe to tag v*\n'
  exit 0
else
  printf 'PREFLIGHT FAILED — fix the items above before tagging\n'
  exit 1
fi
