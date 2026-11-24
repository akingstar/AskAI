#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; cd "$ROOT"
command -v xmllint >/dev/null || { echo "xmllint missing. Install: pkg install libxml2"; exit 2; }
fail=0
while IFS= read -r -d '' f; do xmllint --noout "$f" || fail=1; done < <(find app/src/main -type f -name '*.xml' -print0)
# Verify manifest component declarations map to source files.
while IFS= read -r cls; do
  [[ -z "$cls" ]] && continue
  fqcn="${cls#.}"; fqcn="${fqcn//.//}"
  if [[ "$cls" == .* ]]; then path="app/src/main/java/com/akingstar/askai/${fqcn}"; else path="app/src/main/java/${fqcn}"; fi
  [[ -f "$path.kt" || -f "$path.java" ]] || { echo "MISSING manifest component source: $cls"; fail=1; }
done < <(sed -nE 's/.*android:name="([.A-Za-z0-9_]+)".*/\1/p' app/src/main/AndroidManifest.xml | grep -E '^\.(service\.|MainActivity)' || true)
# Check every referenced R.string and R.drawable symbol exists in resources.
for kind in string drawable; do
  while IFS= read -r name; do
    [[ -z "$name" ]] && continue
    if ! grep -Rqs "name=\"$name\"" app/src/main/res; then echo "MISSING R.$kind.$name"; fail=1; fi
  done < <(grep -Rho "R\.${kind}\.[A-Za-z0-9_]*" app/src/main/java | sed 's/android\.R\.drawable\.//g' | grep -E '^R\.' | sed "s/R\.${kind}\.//" | sort -u)
done
# Lightweight Kotlin delimiter sanity check (not a Kotlin parser/compiler).
while IFS= read -r -d '' f; do
  opens=$(grep -o '{' "$f" | wc -l); closes=$(grep -o '}' "$f" | wc -l)
  [[ "$opens" == "$closes" ]] || { echo "Brace count mismatch: $f ($opens open / $closes close)"; fail=1; }
done < <(find app/src/main/java -type f -name '*.kt' -print0)
if [[ $fail -eq 0 ]]; then echo "PASS: XML parse, manifest component files, R.string/R.drawable references, basic Kotlin brace counts."; else echo "FAIL: see diagnostics above."; exit 1; fi
