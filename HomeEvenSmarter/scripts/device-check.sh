#!/usr/bin/env bash
# Is the attached phone fit for the demo?  scripts/device-check.sh [adb serial]
set -uo pipefail
SERIAL="${1:-}"; ADB=(adb); [ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")
p() { "${ADB[@]}" shell getprop "$1" | tr -d '\r'; }
ok=1
echo "model:     $(p ro.product.model) (Android $(p ro.build.version.release), API $(p ro.build.version.sdk))"
ABI="$(p ro.product.cpu.abilist)"; echo "abis:      $ABI"; case "$ABI" in *arm64-v8a*) ;; *) echo "  !! arm64-v8a required"; ok=0;; esac
MEM=$("${ADB[@]}" shell grep MemTotal /proc/meminfo | awk '{print int($2/1024)}'); echo "ram:       ${MEM} MB"; [ "$MEM" -ge 6000 ] || { echo "  !! 6 GB or more recommended (the NLU needs ~1.5 GB resident)"; ok=0; }
FREE=$("${ADB[@]}" shell df -m /data | awk 'NR==2{print $4}'); echo "free:      ${FREE} MB on /data"; [ "$FREE" -ge 3000 ] || { echo "  !! 3 GB free needed"; ok=0; }
echo "vulkan:    $("${ADB[@]}" shell getprop ro.hardware.vulkan) / gpu $("${ADB[@]}" shell getprop ro.hardware.egl)"
echo "stay awake: $("${ADB[@]}" shell settings get global stay_on_while_plugged_in)"
[ "$ok" = 1 ] && echo "OK" || { echo "NOT OK"; exit 1; }
