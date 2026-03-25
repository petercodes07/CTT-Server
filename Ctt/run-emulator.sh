#!/usr/bin/env bash
set -euo pipefail

AVD_NAME="${1:-Medium_Phone_API_36.1}"
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
EMULATOR_BIN="$SDK_ROOT/emulator/emulator"
ADB_BIN="$SDK_ROOT/platform-tools/adb"
APP_ID="com.example.ctt"
ACTIVITY=".MainActivity"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  echo "Usage: ./run-emulator.sh [AVD_NAME]"
  echo "Default AVD_NAME: Medium_Phone_API_36.1"
  exit 0
fi

if [[ ! -x "$EMULATOR_BIN" ]]; then
  echo "Emulator not found at: $EMULATOR_BIN" >&2
  exit 1
fi

if [[ ! -x "$ADB_BIN" ]]; then
  echo "adb not found at: $ADB_BIN" >&2
  exit 1
fi

if ! "$EMULATOR_BIN" -list-avds | grep -Fxq "$AVD_NAME"; then
  echo "AVD '$AVD_NAME' was not found. Available AVDs:" >&2
  "$EMULATOR_BIN" -list-avds >&2
  exit 1
fi

if ! "$ADB_BIN" get-state >/dev/null 2>&1; then
  echo "Starting emulator: $AVD_NAME"
  nohup "$EMULATOR_BIN" -avd "$AVD_NAME" >/tmp/ctt-emulator.log 2>&1 &
else
  echo "An adb device is already connected. Reusing current device."
fi

echo "Waiting for device..."
"$ADB_BIN" wait-for-device

BOOT_OK=""
for _ in {1..120}; do
  BOOT_OK=$("$ADB_BIN" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [[ "$BOOT_OK" == "1" ]]; then
    break
  fi
  sleep 2
done

if [[ "$BOOT_OK" != "1" ]]; then
  echo "Timed out waiting for emulator boot." >&2
  exit 1
fi

echo "Installing debug build..."
./gradlew installDebug

echo "Launching app..."
"$ADB_BIN" shell am start -n "$APP_ID/$ACTIVITY"

echo "Done."
