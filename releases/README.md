# Galaxy Diablo — Releases

Folder ini mengandungi binari prebuilt untuk install terus tanpa perlu compile.

## Fail

| Fail | Saiz | Penerangan |
|---|---|---|
| `galaxy-diablo-v1.6-debug.apk` | ~12 MB | **Latest** — Playback: ClearKey inline + DRM detection + live buffer tuning + auto-retry. |
| `galaxy-diablo-source-v1.6.zip` | ~160 KB | **Latest** — Source snapshot v1.6. |
| `galaxy-diablo-v1.5-debug.apk` | ~12 MB | v1.5 (security: buang reveal/sembunyi + Reset to Default). |
| `galaxy-diablo-source-v1.5.zip` | ~160 KB | Source snapshot v1.5. |
| `galaxy-diablo-v1.4-debug.apk` | ~12 MB | v1.4 (immersive fullscreen + URL masking). |
| `galaxy-diablo-source-v1.4.zip` | ~280 KB | Source snapshot v1.4. |
| `galaxy-diablo-v1.3-debug.apk` | ~12 MB | v1.3 (player touch fix + UA fix). |
| `galaxy-diablo-source-v1.3.zip` | ~280 KB | Source v1.3. |
| `galaxy-diablo-v1.2-debug.apk` | ~12 MB | v1.2 (Telegram info + auto-refresh). |
| `galaxy-diablo-source-v1.2.zip` | ~150 KB | Source v1.2. |

## Cara recompile sendiri

```bash
# 1. Extract zip
unzip galaxy-diablo-source-v1.2.zip
cd galaxy-diablo

# 2. Build debug APK
./gradlew assembleDebug

# 3. APK akan tersedia di:
#    app/build/outputs/apk/debug/app-debug.apk
```

### Keperluan
- **JDK 17**
- **Android SDK** (compile/target SDK 34, min SDK 21)
- **Gradle 8.7** (auto-download via wrapper)
- **Android Gradle Plugin 8.5.2**

## Versi

**v1.6** — playback fixes (latest)
- ClearKey inline `kid:key` sekarang berfungsi (LocalMediaDrmCallback gantikan `data:` URI yang tertolak)
- DRM auto-detection: proxy ClearKey URL (cumbudrm.php, semar.my.id) di-label betul sebagai ClearKey
- Skip DRM untuk HLS + kid:key (kombinasi yang tak compatible)
- `LoadControl` di-tune untuk live IPTV: minBuffer 2s, maxBuffer 15s, fast tune-in
- Auto-retry sekali pada transient error (network/timeout/manifest)
- HTTP connect & read timeout 20s → 30s

**v1.5** — security hardening
- Buang sepenuhnya ikon mata (reveal/sembunyi) dan baris "Sumber semasa" di Settings
- Buang butang "Reset ke Default" — pulihkan sumber asal memerlukan clear app data dari Tetapan sistem
- Setting page hanya kekal satu input field untuk tukar sumber

**v1.4** — UX + privacy
- Immersive fullscreen mode (status bar + nav bar hidden) untuk semua activity
- URL playlist M3U dimask di Settings; tap ikon mata untuk reveal (digantikan sepenuhnya di v1.5)
- Strings & About text dah dibersihkan dari reference sumber khusus

**v1.3** — bug fixes
- Fix: player control buttons tak respond (transparent overlay blocked touches)
- Add: Chrome-like User-Agent default untuk lebih banyak CDN compatibility
- Add: MimeType detection untuk URL tanpa file extension
- Add: HTTP timeout 20s untuk stream slow-start
- Improve: error message tunjuk nama channel + cause

**v1.2** — auto-refresh komprehensif + Settings Info section
- Stale-while-revalidate (background fetch fresh tiap kali buka)
- Periodic auto-refresh setiap 30 minit semasa app foreground
- Pull-to-refresh pada channel grid
- Settings → Info & Hubungi → Telegram clickable (@suzuneiayano)

**v1.1** — Settings Info section (Telegram)

**v1.0** — Build pertama
- Media3 ExoPlayer (DASH + HLS + Widevine + ClearKey)
- Neon-blue glassmorphism UI
- Dual M3U parser (standard + `=== Kategori ===` custom format)
- Splash screen + first-time welcome toast
- Landscape-only TV-focused

© Suzuneiayano
