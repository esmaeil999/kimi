#!/usr/bin/env bash
# اجرای standalone استراتژی
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p output
java -Dfile.encoding=UTF-8 -cp "bin:lib/*" StrategyRunner
