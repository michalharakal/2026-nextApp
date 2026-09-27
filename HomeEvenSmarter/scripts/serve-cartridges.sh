#!/usr/bin/env bash
# Serve the materialized cartridges to the phone over WiFi.  scripts/serve-cartridges.sh [dir] [port]
# The server prints the URLs to type into the app's Cartridges screen.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$(cd "${1:-$HERE/build/cartridges}" 2>/dev/null && pwd)" || { echo "no cartridges at ${1:-$HERE/build/cartridges} — run scripts/materialize.sh first"; exit 1; }
PORT="${2:-8080}"
cd "$HERE" && CARTRIDGES_DIR="$DIR" PORT="$PORT" exec ./gradlew -q :server:run
