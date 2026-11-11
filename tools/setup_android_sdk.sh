#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
mkdir -p "$SDK_ROOT/cmdline-tools"
if [[ ! -d "$SDK_ROOT/cmdline-tools/latest" ]]; then
  ARCHIVE="/tmp/cmdline-tools.zip"
  curl -fsSL -o "$ARCHIVE" https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  rm -rf /tmp/cmdline-tools
  unzip -q "$ARCHIVE" -d /tmp
  mv /tmp/cmdline-tools "$SDK_ROOT/cmdline-tools/latest"
fi
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/build-tools/37.0.0:$SDK_ROOT/platform-tools:$PATH"
set +o pipefail
yes | sdkmanager --licenses >/dev/null || true
set -o pipefail
sdkmanager --install "platforms;android-37.0" "build-tools;37.0.0" "platform-tools" >/dev/null
if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "ANDROID_SDK_ROOT=$SDK_ROOT" >> "$GITHUB_ENV"
  echo "ANDROID_HOME=$SDK_ROOT" >> "$GITHUB_ENV"
  echo "PATH=$PATH" >> "$GITHUB_ENV"
fi
