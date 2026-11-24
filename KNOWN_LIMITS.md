# Known limits — read before installing

This is a first repository foundation. Do not present it as a complete elder-care phone automation product yet.

- Android SDK/Gradle compilation and real-device tests have not been run in the authoring environment.
- Speech input uses Android `RecognizerIntent`; true bundled Vosk Hindi/English offline speech models and first-run model download/progress UI are not included. Device offline speech support varies.
- Gemini/Groq endpoints and model IDs are currently defined in the AI router source, not a user-editable endpoint/model configuration screen. There is no JSON repair retry yet; invalid model output falls back to local rules.
- Offline parser handles a small command set only. It does not reliably extract contacts, nicknames, durations, addresses or complex multi-step messages.
- Calls and SOS are deliberately not executed automatically. The current safety gate stops those actions pending a complete contact/confirmation/PIN implementation.
- WhatsApp shortcut opens WhatsApp only. It does not select a contact, type or send messages, initiate calls or video calls.
- Camera, alarm, YouTube and Maps use Android intents. They do not verify the destination screen tree or retry alternate UI paths.
- SOS does not send SMS, share location or call family. It reports that setup/integration is required.
- Family Setup is not a finished wizard. There is no actual Family PIN lock, contacts/nicknames database, medication reminder scheduler, saved home address or permission-status dashboard.
- Accessibility service is declared and can read a visible text tree in its service class, but the app does not yet expose screen reading/tap/scroll/type/back/home controls through a complete verified executor. Android secure screens (`FLAG_SECURE`) cannot be read or captured.
- Notification listener is declared but notification display/reply workflow is not implemented.
- Floating orb, routines/macros, always-on wake word, microphone foreground-service start/stop UI, robust boot recovery and OEM-specific autostart flows are not implemented.
- Android restricts background microphone use, direct Wi-Fi/Bluetooth/DND changes, SMS/call permissions, notification access and restricted settings. The app cannot bypass those restrictions.
- No Play Store policy review, security audit, accessibility audit or penetration test has been performed.

Do not release a build to vulnerable users until the remaining safety, privacy, contact confirmation and emergency workflows are implemented and tested on real devices.
