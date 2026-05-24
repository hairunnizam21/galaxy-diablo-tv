# Galaxy Diablo

Galaxy-themed IPTV player for Android. Replaces a PerfectTV-style player with a
modern AndroidX Media3 (ExoPlayer) backend and a neon-blue glassmorphism UI.

> Universe of Live TV — © Suzuneiayano

---

## ⬇️ Download (latest v1.2)

| | Fail | Saiz | Link |
|---|---|---|---|
| 📱 | **APK siap install** | ~12 MB | **[Download APK](releases/galaxy-diablo-v1.2-debug.apk)** |
| 📦 | **Source code zip** (untuk recompile) | ~150 KB | **[Download ZIP](releases/galaxy-diablo-source-v1.2.zip)** |

> **Cara install APK:** Buka link di telefon Android → benarkan "Install dari sumber tidak dikenali" → tap fail yang dimuat turun → Install.

Lihat folder [`releases/`](releases/) untuk semua versi.

---

## Features

- **AndroidX Media3 ExoPlayer 1.4.1** — same engine family as the source app.
- **DASH (.mpd)**, **HLS (.m3u8)**, **MP4 progressive**, **RTMP** stream support.
- **DRM**: Widevine + ClearKey (auto-detected from playlist URLs and `#KODIPROP`
  / `#EXTVLCOPT` directives).
- **Dual playlist parser**: standard M3U (`#EXTM3U` + `#EXTINF`) **and** the
  custom `=== Category ===` block format used by `animedantv.m3u`.
- **Top-bar**: search, refresh, clear-cache, settings, exit.
- **Sidebar**: categories auto-generated from `=== ... ===` headers in the
  playlist, plus "Semua" (All) and "Kegemaran" (Favorites).
- **Channel grid** with neon-blue glass cards. Channels without a logo still
  show the channel name (monogram + name fallback) and can still be played.
- **Player controls** mirroring the source app: prev / next channel,
  Picture-in-Picture, video / audio / subtitle pickers, aspect-ratio cycle,
  EPG (Now/Next placeholder ready), focus-driven navigation for TV remotes.
- **Splash screen** with branded loading animation and progress while the
  playlist is fetched from GitHub.
- **First-time welcome toast** ("Pengenalan App / Sumber Link Free Source"),
  dismissable via a Settings toggle.
- **Custom playlist URL** input in Settings — paste any M3U source URL from
  GitHub or elsewhere; auto-detects the format. Default falls back to
  the configured `animedantv.m3u`.
- **Landscape-only** orientation, TV-friendly focus highlights.

## Build

```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Requires Android SDK with build-tools 34, JDK 17, Gradle 8.7
(auto-downloaded by the wrapper).

## Playlist source

Default playlist URL is configured at build time
(`BuildConfig.PLAYLIST_URL`):

> `https://raw.githubusercontent.com/hairunnizam21/myiptv-playlist/refs/heads/main/animedantv.m3u`

Users can override it at runtime via **Settings → URL Playlist M3U**.

## License

© Suzuneiayano. Source streams are sourced from public free sources via the
configured M3U playlist. The application code is provided as-is.
