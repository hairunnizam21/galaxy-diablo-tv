# Galaxy Diablo — Releases

Folder ini mengandungi binari prebuilt untuk install terus tanpa perlu compile.

## Fail

| Fail | Saiz | Penerangan |
|---|---|---|
| `galaxy-diablo-v1.2-debug.apk` | ~12 MB | APK debug siap pasang. Install terus pada Android (Aktifkan "Install from Unknown Sources" dahulu). |
| `galaxy-diablo-source-v1.2.zip` | ~150 KB | Snapshot source code lengkap untuk recompile di Android Studio. |

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
