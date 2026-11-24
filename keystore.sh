#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
pkg install -y openjdk-17 coreutils
OUT="${1:-askai-release.jks}"
read -r -p "Key alias [askai]: " ALIAS; ALIAS="${ALIAS:-askai}"
read -r -s -p "Keystore password (min 6 chars): " STOREPASS; echo
read -r -s -p "Key password (min 6 chars): " KEYPASS; echo
if [[ ${#STOREPASS} -lt 6 || ${#KEYPASS} -lt 6 ]]; then echo "Passwords must be at least 6 characters." >&2; exit 1; fi
keytool -genkeypair -v -keystore "$OUT" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 -storepass "$STOREPASS" -keypass "$KEYPASS" -dname "CN=Ask AI, OU=Android, O=Ask AI, L=Unknown, ST=Unknown, C=IN"
echo
echo "GitHub repository Settings > Secrets and variables > Actions me ye secrets banao:"
echo "ANDROID_KEYSTORE_BASE64 = $(base64 -w0 "$OUT")"
echo "ANDROID_KEYSTORE_PASSWORD = (your keystore password)"
echo "ANDROID_KEY_ALIAS = $ALIAS"
echo "ANDROID_KEY_PASSWORD = (your key password)"
echo "WARNING: Keystore ka offline backup zaroor rakho. Kho gaya to isi signing identity se future APK updates release nahi kar paoge."
echo "Do not upload the .jks file or passwords to GitHub."
