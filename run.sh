#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.tigerpaw.launcher.debug"
ACTIVITY="com.tigerpaw.launcher/.MainActivity"

echo "==> Building debug APK..."
./gradlew assembleDebug

echo "==> Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "==> Launching..."
adb shell am start -n "$ACTIVITY"

echo "==> Done."
