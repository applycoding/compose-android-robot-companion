#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
if [[ ! -f "$APK" ]]; then
  echo "missing APK: $APK" >&2
  exit 1
fi
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
AAPT2="$SDK_ROOT/build-tools/37.0.0/aapt2"
if [[ ! -x "$AAPT2" ]]; then
  echo "missing aapt2: $AAPT2" >&2
  exit 1
fi
BADGING="$("$AAPT2" dump badging "$APK")"
echo "$BADGING"
echo "$BADGING" | grep -q "package: name='com.applycoding.robot_companion'"
echo "$BADGING" | grep -q "application-debuggable"
echo "$BADGING" | grep -q "minSdkVersion:'26'"
echo "$BADGING" | grep -q "targetSdkVersion:'37'"
