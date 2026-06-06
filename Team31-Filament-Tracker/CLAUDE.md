# CLAUDE.md — Project Instructions

## Project
**BSM — Bambu Spool Manager**
An Android + web app to read Bambu Lab filament spool NFC tags, manage filament inventory, and print labels via Fichero D11s thermal printer.
This project is **open source** — all code, docs, and setup instructions must be clear enough for external contributors.

## Documentation Requirements
- Keep README.md up to date with every significant change (new features, setup steps, architecture changes)
- Document all setup steps (Firebase, Android Studio, NFC, web app, Fichero) so a new developer can get running from scratch
- Add inline code comments only where logic is non-obvious (e.g., NFC key derivation, MIFARE block parsing, label rendering)
- When adding a new dependency, document why it's needed in README.md

## Code Style & Conventions

### Android App
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM (ViewModel + StateFlow + Compose)
- DI: Hilt
- Local DB: Room
- Cloud: Firebase Firestore (offline-first sync)
- Min SDK: 24, Target SDK: 35
- Package: `com.bambu.nfc`

### Web App
- Single-page HTML app (no build step)
- Firebase SDK for auth + Firestore
- Fichero printer library bundled via esbuild (see `web/build-fichero.sh`)
- Label canvas: 232x96px (30x14mm at 203 DPI)
- Print direction: "left" (canvas rotated 90° CW before rasterizing)

## Key Technical Details

### NFC Tag Format (MIFARE Classic 1K)
- 16 sectors, 4 blocks/sector, 16 bytes/block
- Authentication: HKDF-SHA256 key derivation from 4-byte UID
  - Salt: `9a759cf2c4f7caff222cb9769b41bc96`
  - Info: `"RFID-A\0"` (ASCII with null terminator)
  - Single HKDF expand produces 96 bytes (16 keys x 6 bytes)
- Key blocks: 1 (material), 2 (type), 4 (detailed type), 5 (color/weight/diameter), 6 (temps), 9 (tray UID), 12 (production date), 14 (length)

### Inventory Model
- Each physical spool tracked individually with auto-incrementing ID (#001, #002, ...)
- Statuses: `IN_STOCK` → `IN_USE` → `USED_UP`
- Used-up spools kept in history for reorder reference
- Matching existing spools by `trayUid` field from NFC tag
- User always confirms actions after scanning (no auto-processing)

### Label Printing (Fichero D11s)
- Printhead: 96px wide, 203 DPI
- Label stickers: 30x14mm (232x96px canvas)
- Library: [fichero-printer](https://github.com/0xMH/fichero-printer) by @0xMH
- Bundled via esbuild to `web/public/fichero.js`
- Anti-aliased text must be thresholded to pure black/white before encoding (ImageEncoder only treats R=0 as black)

### Color Names
- Bambu Lab official color hex codes mapped to names (Cyan, Marine Blue, Ice Blue, etc.)
- Source: Bambu Lab hex code tables + printbusters.io
- Nearest-match fallback within RGB distance of 30

### Firebase
- Project hosting: Firebase Hosting
- Auth: Google sign-in
- Database: Firestore (offline-first)
- `google-services.json` is gitignored — see `.json.example`

## Build & Run
```bash
# Android
./gradlew assembleDebug
./gradlew installDebug

# Web — rebuild Fichero library (only when upstream changes)
cd web && bash build-fichero.sh

# Web — deploy
cd web && firebase deploy --only hosting
```
