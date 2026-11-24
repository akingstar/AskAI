#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
command -v git >/dev/null || { echo "Install git: pkg install git"; exit 1; }
read -r -p "GitHub username [akingstar]: " GH_USER; GH_USER="${GH_USER:-akingstar}"
read -r -p "Repository name [AskAI]: " GH_REPO; GH_REPO="${GH_REPO:-AskAI}"
read -r -s -p "GitHub token (hidden input): " GH_TOKEN; echo
[[ -n "$GH_TOKEN" ]] || { echo "Token cannot be empty"; exit 1; }
ASKAI_ASKPASS="$(mktemp)"; trap 'rm -f "$ASKAI_ASKPASS"; unset GH_TOKEN' EXIT
cat > "$ASKAI_ASKPASS" <<'ASKPASS'
#!/data/data/com.termux/files/usr/bin/sh
case "$1" in *Username*) echo "x-access-token";; *) printf '%s' "$GH_TOKEN";; esac
ASKPASS
chmod 700 "$ASKAI_ASKPASS"
git init -b main
git config user.name "${GH_USER}"
git config user.email "${GH_USER}@users.noreply.github.com"
git add -A
git diff --cached --quiet || git commit -m "Initial Ask AI Android assistant project"
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/${GH_USER}/${GH_REPO}.git"
export GH_TOKEN GIT_ASKPASS="$ASKAI_ASKPASS" GIT_TERMINAL_PROMPT=0
git push -u origin main
echo "Push complete. GitHub Actions tab me APK build dekhein."
