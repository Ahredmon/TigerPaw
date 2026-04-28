#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# TigerPaw — Build & Emulate Script
#
# Usage:
#   ./emulate.sh [AVD_NAME]
#
# Examples:
#   ./emulate.sh                     # auto-pick first available AVD
#   ./emulate.sh Pixel_9_API_35      # boot a specific AVD
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

PACKAGE="com.tigerpaw.launcher"
ACTIVITY="${PACKAGE}/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

# ── Helpers ───────────────────────────────────────────────────────────────────
log()  { echo -e "\033[1;36m▶ $*\033[0m"; }
ok()   { echo -e "\033[1;32m✔ $*\033[0m"; }
die()  { echo -e "\033[1;31m✘ $*\033[0m" >&2; exit 1; }

require() { command -v "$1" &>/dev/null || die "Required tool not found: $1 — is ANDROID_HOME set and SDK tools on PATH?"; }

require adb
require emulator
require avdmanager

# ── Pick AVD ──────────────────────────────────────────────────────────────────
REQUESTED_AVD="${1:-}"

# List available AVDs (strip leading whitespace/dashes emulator -list-avds outputs)
mapfile -t AVDS < <(emulator -list-avds 2>/dev/null | sed 's/^[[:space:]]*//' | grep -v '^$')

[[ ${#AVDS[@]} -gt 0 ]] || die "No AVDs found. Create one with: avdmanager create avd -n Pixel_9_API_35 -k 'system-images;android-35;google_apis_playstore;x86_64'"

if [[ -n "$REQUESTED_AVD" ]]; then
    # Validate the requested AVD exists
    found=0
    for avd in "${AVDS[@]}"; do
        [[ "$avd" == "$REQUESTED_AVD" ]] && found=1 && break
    done
    [[ $found -eq 1 ]] || die "AVD '$REQUESTED_AVD' not found. Available AVDs:\n$(printf '  %s\n' "${AVDS[@]}")"
    AVD="$REQUESTED_AVD"
else
    AVD="${AVDS[0]}"
    log "Auto-selected AVD: $AVD"
fi

# ── Check if emulator is already running ──────────────────────────────────────
EMULATOR_SERIAL=""
while IFS= read -r line; do
    serial=$(echo "$line" | awk '{print $1}')
    if [[ "$serial" == emulator-* ]]; then
        running_avd=$(adb -s "$serial" emu avd name 2>/dev/null | head -1 | tr -d '\r' | sed 's/^[[:space:]]*//')
        if [[ "$running_avd" == "$AVD" ]]; then
            EMULATOR_SERIAL="$serial"
            break
        fi
    fi
done < <(adb devices 2>/dev/null | tail -n +2 | grep -v '^$')

if [[ -n "$EMULATOR_SERIAL" ]]; then
    ok "Emulator already running: $AVD ($EMULATOR_SERIAL)"
else
    log "Starting emulator: $AVD…"
    emulator -avd "$AVD" -no-snapshot-load -no-audio &
    EMULATOR_PID=$!

    # Wait for a new emulator-* serial to appear
    log "Waiting for emulator device to appear…"
    for i in $(seq 1 30); do
        EMULATOR_SERIAL=$(adb devices 2>/dev/null | grep '^emulator-' | awk '{print $1}' | head -1)
        [[ -n "$EMULATOR_SERIAL" ]] && break
        sleep 2
    done
    [[ -n "$EMULATOR_SERIAL" ]] || { kill "$EMULATOR_PID" 2>/dev/null; die "Emulator device never appeared."; }

    # Wait for boot to complete
    log "Waiting for Android boot (serial: $EMULATOR_SERIAL)…"
    adb -s "$EMULATOR_SERIAL" wait-for-device
    for i in $(seq 1 60); do
        boot=$(adb -s "$EMULATOR_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        [[ "$boot" == "1" ]] && break
        sleep 2
    done
    [[ "$boot" == "1" ]] || { kill "$EMULATOR_PID" 2>/dev/null; die "Emulator booted but Android never finished starting."; }
    ok "Emulator booted: $AVD"
fi

# ── Build ─────────────────────────────────────────────────────────────────────
log "Building debug APK…"
./gradlew :app:assembleDebug
[[ -f "$APK" ]] || die "APK not found at $APK"

# ── Install ───────────────────────────────────────────────────────────────────
log "Installing APK on $EMULATOR_SERIAL…"
adb -s "$EMULATOR_SERIAL" install -r "$APK"
ok "Installed"

# ── Launch ────────────────────────────────────────────────────────────────────
log "Launching $PACKAGE…"
adb -s "$EMULATOR_SERIAL" shell am start -n "$ACTIVITY"
ok "Done — TigerPaw is running on $AVD"
