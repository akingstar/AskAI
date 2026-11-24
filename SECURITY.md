# Security policy

Report security issues privately to the repository owner; do not publish API keys, personal data, phone numbers, screenshots of private notifications or exploit details in public issues.

- Never commit API keys, signing keystores, signing passwords, family PINs or personal contact data.
- GitHub signing secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Protect the keystore with at least two offline backups. Losing it prevents updates signed with the same identity.
- Treat Accessibility and Notification access as high-risk permissions. Do not enable them on a device without informed owner consent.
- Never collect, speak or type OTPs, PINs or passwords. Stop on payment, banking, credential and secure/FLAG_SECURE screens.
- Validate every model-generated action against a strict allow-list and require confirmation for sensitive actions. The current build intentionally blocks call/SOS execution pending a complete safety flow.
- This project has not had an independent security audit.
