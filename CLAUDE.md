# Groundwork

Android app for team leadership workflows — 1on1 templates, PDF management, Google Drive sync. Optimized for Boox e-ink devices (Air 4C, Palma 2 Pro).

## Project

- **App name**: Groundwork
- **Package**: `com.groundwork.programmieramt`
- **Owner**: Sebastian (bibabutzi@gmail.com)

## Build

```powershell
# Debug APK (no keystore needed)
./gradlew assembleProdDebug

# Release APK (needs keystore.jks at project root)
$env:KEYSTORE_PASSWORD = "android"; $env:KEY_PASSWORD = "android"
./gradlew assembleProdRelease
```

APK output: `app/build/outputs/apk/prod/`

## Google Drive

- OAuth via legacy `GoogleSignIn` API (play-services-auth)
- Scopes: `DRIVE_APPDATA`, `DRIVE_FILE`
- `google-services.json` is gitignored — must be added locally before building
- To set up: Google Cloud Console → new project → enable Drive API → create OAuth Android client for `com.groundwork.programmieramt` with SHA-1 from `~/.android/debug.keystore`

### Get SHA-1 of debug keystore

```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

## ADB

Full path (winget install):
```
C:\Users\note\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe
```

## Boox SDK

Repo: `http://repo.boox.com/repository/maven-public/` (HTTP, allowInsecureProtocol = true)
Dependencies: `onyxsdk-base:1.8.2.1`, `onyxsdk-device:1.3.1.3`

## Signing

Uses `~/.android/debug.keystore` (alias: `androiddebugkey`, password: `android`).
For CI: add keystore as `KEYSTORE_BASE64` secret in GitHub → Environment "Prod".

## Reference project

Forked from toolsboox-android → see `c:\Users\note\Documents\coding\toolsboox-android` for Drive sync patterns, Boox e-ink rendering, and calendar plugin examples.
