> ⚠️ **This repository is superseded.** Development continues in [SaliSalvia/SalviaBrowxer](https://github.com/SaliSalvia/SalviaBrowxer) — the current, maintained browser+downloader. Please use that repository for the latest code and releases.

# 🚀 Browxer — Android Media Browser & Downloader

A WebView-based Android browser whose superpower is an **evidence-based Media Sniffer Engine**: it detects and downloads the *primary* video/audio/image the user actually came for — including dynamic sites (Instagram, TikTok, Twitter/X, YouTube, Facebook, Reddit, Vimeo) — with honest, no-fake-download guardrails.

## Stack
Kotlin · Jetpack Compose (Material 3) · Clean Architecture + MVVM/MVI · Hilt · Coroutines/Flow · Room · DataStore · MediaStore · WorkManager · Foreground Service (dataSync) · Media3 ExoPlayer · youtubedl-android (yt-dlp + FFmpeg + aria2c) · jsoup · OkHttp 4.12

## Triple-Layer Sniffing
1. **Network interception** — `shouldInterceptRequest` + `onLoadResource`, MIME/extension/header filtering
2. **DOM/JS injection** — `media_sniffer.js`: video/audio/source/img/preload, og:video, og:image, twitter:player:stream, JSON-LD contentUrl/embedUrl, MutationObserver (SPA), fetch/XHR hooks, same-origin iframe recursion
3. **Blob handler** — `URL.createObjectURL` override; 512 KB chunks; 50 MB cap (honest "blob too large" report)

## Honest Guardrails (never fake a download)
- Widevine/PlayReady DASH → `DashDrmException` → "This media is DRM-protected and cannot be downloaded."
- Live playlists (no `EXT-X-ENDLIST`, not `PLAYLIST-TYPE:VOD`) → `HlsLiveException`
- AES-128/SAMPLE-AES HLS → `HlsEncryptedException`
- Idle FAB stays gray → "Scanning… no playable media detected yet."
- `@JavascriptInterface` hardened against CVE-2024-46961: primitive args, 8 KB caps, scheme allow-list, JSON-only parsing, background HandlerThread, no eval

## Ranking (production table)
OG/Twitter/JSON-LD **100** · YTDLP **95** · HLS variant **90** · DASH rep **85** · DOM video/audio **70** · DOM image ≥800px **50** · network XHR **40** · blob **35**
Penalties: ad domain **-80** · ad keywords **-60** · <720p-if-higher-exists **-30** · duration<3s **-40** · watermark/logo **-25** · top score <60 → yt-dlp fallback

## Module Map
`app` · `core:common/domain/database/data/datastore/di/media/network/ui` · `engine:sniffer/ranker/resolver/downloader/muxer` · `feature:browser/download_sheet/library/player/settings`

## Status
Implemented: 14 production deliverables (settings, manifest, Application, sniffer JS + bridge, admission + ranker, domain models, Direct/HLS/DASH/yt-dlp/Blob resolvers, download service with 3-slot semaphore + Wi-Fi gate, aria2c/FFmpeg mux, Room + recovery worker, VM + states, browser + quality-sheet UI, Hilt modules, EN+FA strings).

Honest stubs (marked, never hidden): DASH SegmentTemplate URL templating (representation-level URLs assumed), HLS segment concurrency pool (currently sequential), notification retry action TODO, per-module `build.gradle.kts` wiring left to host conventions.

> Scaffolded by Vibe from the Browxer production blueprint. Min SDK 24 · Target SDK 35 · JDK 17.
