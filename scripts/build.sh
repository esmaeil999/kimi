#!/usr/bin/env bash
# کامپایل پروژه داخل پوشه‌ی bin/
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin src/main/java/*.java
echo ">> Build OK"
