#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.tigerpaw.launcher.debug"
ACTIVITY="com.tigerpaw.launcher.debug/com.tigerpaw.launcher.MainActivity"

echo "==> Building debug APK..."
./gradlew assembleDebug

echo "==> Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "==> Granting permissions..."
adb shell pm grant "$PACKAGE" android.permission.INTERNET              2>/dev/null || true
adb shell pm grant "$PACKAGE" android.permission.ACCESS_NETWORK_STATE  2>/dev/null || true
adb shell pm grant "$PACKAGE" android.permission.CHANGE_NETWORK_STATE  2>/dev/null || true
adb shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb shell pm grant "$PACKAGE" android.permission.READ_MEDIA_IMAGES      2>/dev/null || true

echo "==> Stopping any existing instance..."
adb shell am force-stop "$PACKAGE" || true

echo "==> Launching..."
adb shell am start -n "$ACTIVITY"

# Give the app a moment to start, then clear stale logs and stream
sleep 1
adb logcat -c
echo "==> Streaming logcat for $PACKAGE (Ctrl+C stops both logcat and scrcpy)..."
adb logcat -v time 2>/dev/null | grep --line-buffered "$PACKAGE" &
LOGCAT_PID=$!

echo "==> Mirroring screen (press Ctrl+C to stop)..."
scrcpy --stay-awake || true

# Clean up logcat when scrcpy exits
kill "$LOGCAT_PID" 2>/dev/null || true
wait "$LOGCAT_PID" 2>/dev/null || true

echo "==> Done."
