# Privacy notice

Ask AI is designed for local-first assistance. This early build has no account system, analytics SDK, advertising SDK or app-owned tracking endpoint.

- Voice recognition uses Android's `RecognizerIntent`; depending on the device and recognition provider, audio or transcripts may be processed by Google or another system speech service. Offline recognition is not guaranteed.
- If a user adds a Gemini or Groq key, the command text sent to that provider is processed under that provider's terms and privacy policy. Ask AI does not control provider retention or free-tier training rules.
- Command history is stored locally in the app's Room database. Clearing app data removes it.
- API keys are stored using AndroidX Security Crypto backed by Android Keystore. This library version is an alpha release; treat the device lock and OS security as important protections.
- Accessibility and Notification access can expose on-screen text and notifications. Enable only for a person who understands and needs those features. This foundation does not upload screen trees or notifications.
- Contacts, SMS, calls, location, camera, overlay and microphone are not silently accessed. Android permissions and settings remain under the device owner's control.
- Do not use Ask AI to disclose OTPs, passwords, PINs or financial secrets. The assistant should stop at payment/banking authentication screens.

This notice describes the code currently included, not every feature envisioned for future versions. Review changes before release.
