# MotivaWall

MotivaWall is a native Android wallpaper app for turning images and local PDFs into a premium, motivational lock screen. It is intentionally local-only: there are no downloads, remote feeds, accounts, analytics, or web views.

## Included

- Kotlin + Jetpack Compose Material 3
- MVVM-style state with Hilt and Room
- Image picker with crop ratios, rotate/flip, brightness, contrast, saturation, vignette, and quote overlay
- PDF picker using Android's native `PdfRenderer`, page preview, page count guard (1–100), and automatic page rotation
- Home/lock/both wallpaper targets
- Wallpaper history with favorites, filters, delete, and clear
- Foreground PDF rotation service
- Lock-screen floating PDF controls with previous/next, pause/resume, progress, notification reopen, drag-to-position, and haptic feedback
- ARM64-v8a-only release configuration

## Build

Install JDK 17 and Gradle 8.7 or newer, then run:

```bash
cd MotivaWall
gradle assembleRelease
```

The release APK is created under `app/build/outputs/apk/arm64-v8a/release/`.

The release signing configuration points at `app/release-key.jks`, using the uploaded keystore and the requested alias. Keep this repository private because the requested workflow intentionally keeps signing values in source.

## Device notes

- Android may require the user to grant “Display over other apps” before the floating PDF controls can appear.
- The system wallpaper service determines how lock-screen wallpaper behaves on each device vendor.
- PDF rotation and history read only the persisted local URI and app-private files.