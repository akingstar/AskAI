# Ask AI — Bolo, ho jayega

Hindi-first Android voice assistant. Package: `com.akingstar.askai`.

## Build
Push this repository to GitHub. GitHub Actions uses Temurin Java 17 and Gradle 8.7 to run `gradle assembleDebug`. Download the `AskAI-debug-apk` artifact from the workflow run.

## Phone setup
Install the APK, open Ask AI, grant microphone permission, and use the microphone button. Accessibility and notification access are optional and must be enabled manually in Android Settings. Some OEMs require allowing restricted settings for sideloaded apps.

## Limits
Android prevents ordinary apps from silently toggling Wi-Fi, Bluetooth, mobile data, GPS, airplane mode, and hotspot. Ask AI opens the relevant settings screens. Call control and background microphone use are restricted by Android version and permissions. Payment confirmation and factory reset are not supported.

## Privacy
Speech recognition may use the device's configured recognition provider. The optional Worker receives the user's command text. Do not send passwords, OTPs, PINs, or payment credentials.
