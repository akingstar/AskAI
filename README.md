# Ask AI — बोलो, हो जाएगा

Hindi-first, voice-first Android helper for older adults and smartphone beginners. Package: `com.akingstar.askai`.

> **Build status:** This repository is an early functional foundation, not a finished hands-free phone agent. The offline command parser, speech input, Hindi TTS, basic app intents, Room history, encrypted API-key storage, Accessibility service declaration, notification-listener declaration and GitHub build pipeline are present. Several requested integrations remain partial; see [KNOWN_LIMITS.md](KNOWN_LIMITS.md). No Android SDK build was run by the authoring environment.

## Download latest APK

[Open Ask AI Releases](https://github.com/akingstar/AskAI/releases/latest)

Pushes to `main` create a debug APK artifact in Actions. Version tags (`v*`) create a signed release only after all four signing secrets are configured.

## Build from GitHub Actions

1. Create a public repository named `AskAI` under your GitHub account.
2. Push this project using `./push.sh` (see below).
3. Open repository → **Actions** → **Android APK**. Download `askai-apk-*` artifact when the run succeeds.
4. For a public Release, create signing secrets, then push a version tag such as `v0.1.0`.

## Install APK — Hindi

1. GitHub Releases se APK download karein.
2. Android ke download notification/file manager se APK kholein.
3. Android agar pooche to **Install unknown apps / Allow from this source** sirf us browser/file manager ke liye manually allow karein jahan se APK khol rahe hain.
4. Play Protect warning aaye to publisher/source aur SHA-256 verify karein. Warning ko blindly ignore na karein.
5. Kuch Android versions sideloaded apps ke liye **Allow restricted settings** maangte hain: App info → three-dot menu → Allow restricted settings. Yeh step Android khud dikhaye tabhi karein; Ask AI ise bypass nahi karta.
6. Accessibility, Notification access, Overlay aur doosri permissions Settings me manually enable hoti hain. Family member pehle setup kare.

## Install APK — English

1. Download the APK from the official Releases page.
2. Open it from Downloads and allow installation for that specific source if Android asks.
3. Verify the SHA-256 checksum before installing.
4. If Android shows **Allow restricted settings**, review the app and enable it manually from App info → three-dot menu only if you trust the build.
5. Grant Accessibility, Notification access, Overlay and other permissions manually, one at a time.

## Verify SHA-256

Download the `.sha256` file beside the release APK. In Termux, run:

```bash
sha256sum -c app-release.apk.sha256
```

Or calculate the APK hash with `sha256sum app-release.apk` and compare every character with the checksum shown in the GitHub Release.

## Termux: push project

```bash
pkg update -y && pkg install -y git bash coreutils
cd ~/AskAI
bash push.sh
```

Create the empty public repository on GitHub before running `push.sh`. Use a fine-grained token with **Contents: Read and write** and **Workflows: Read and write** for that repository. Token is requested through hidden terminal input and is not written into a project file.

## Local checks

```bash
pkg install -y libxml2
bash check.sh
```

`check.sh` performs XML parsing, checks manifest component source files, checks `R.string`/`R.drawable` references and performs a lightweight Kotlin brace-count check. It is not a substitute for Gradle compilation, lint or device testing.

## Signing keystore

Run `bash keystore.sh` in Termux. Add these exact GitHub Actions secrets under Settings → Secrets and variables → Actions:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Keep at least two offline backups of the `.jks` file. If it is lost, you cannot publish updates signed with that same identity. Never commit it.

## Create a version release

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag workflow creates a GitHub Release only when all four signing secrets are valid. Without them, it still builds/uploads the debug APK and does not fail just because signing is absent.

## API keys (optional)

The app can store Gemini and Groq keys in Android encrypted preferences. They are optional; offline rules still work. Never paste a key into an issue, public chat, screenshot or Git commit. Free tiers have quotas and can change; no unlimited AI usage is promised. Provider policy, retention and commercial-use terms must be reviewed before distributing a build that uses customer-provided keys.

## License

MIT. See [LICENSE](LICENSE).
