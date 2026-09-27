#!/usr/bin/env bash
# Fallback without network: push the materialized pack_dirs into the app's cartridge directory with adb.
#   scripts/sideload-cartridges.sh [build/cartridges] [adb serial]
# The app reads <filesDir>/cartridges/<id>; run-as copies them there (debug build).
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DIR="${1:-$HERE/build/cartridges}"; SERIAL="${2:-}"
PKG=sk.ainet.examples.smarthome
ADB=(adb); [ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")
[ -d "$DIR" ] || { echo "no cartridges at $DIR"; exit 1; }
for pack in "$DIR"/*/; do
  id="$(basename "$pack")"
  echo "==> $id"
  "${ADB[@]}" shell mkdir -p "/data/local/tmp/cartridges/$id"
  "${ADB[@]}" push "$pack" "/data/local/tmp/cartridges/" >/dev/null
  "${ADB[@]}" shell run-as "$PKG" mkdir -p "files/cartridges"
  "${ADB[@]}" shell "run-as $PKG rm -rf files/cartridges/$id; run-as $PKG cp -r /data/local/tmp/cartridges/$id files/cartridges/$id"
  "${ADB[@]}" shell rm -rf "/data/local/tmp/cartridges/$id"
done
echo "done — open the Cartridges screen and press Rescan"
