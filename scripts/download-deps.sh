#!/usr/bin/env bash
# دانلود کتابخانه‌ی JForex API (jforexlib.jar) داخل پوشه‌ی lib/
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p lib

URLS=(
  "https://www.dukascopy.com/client/jforexlib.jar"
  "http://www.dukascopy.com/client/jforexlib.jar"
)

ok=""
for u in "${URLS[@]}"; do
  echo ">> Trying $u"
  if curl -fL --connect-timeout 20 --retry 2 -o lib/jforexlib.jar "$u"; then
    ok="$u"
    break
  fi
done

if [ -z "$ok" ]; then
  echo "ERROR: could not download jforexlib.jar automatically."
  echo "Download the JForex SDK manually from Dukascopy and place jforexlib.jar in ./lib"
  exit 1
fi

echo ">> Downloaded jforexlib.jar from $ok"

# slf4j (در صورت نیاز — معمولاً داخل jforexlib وجود دارد؛ خطا مهم نیست)
SLF4J_VERSION=1.7.36
for a in slf4j-api slf4j-simple; do
  curl -fsSL --connect-timeout 20 -o "lib/$a-$SLF4J_VERSION.jar" \
    "https://repo1.maven.org/maven2/org/slf4j/$a/$SLF4J_VERSION/$a-$SLF4J_VERSION.jar" || true
done

ls -lh lib/
