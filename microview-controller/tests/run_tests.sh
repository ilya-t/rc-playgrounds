#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
javac -d "$TMP_DIR" \
  "$ROOT/android/app/src/main/java/dev/rcplaygrounds/microview/ControlState.java" \
  "$ROOT/android/app/src/main/java/dev/rcplaygrounds/microview/Protocol.java" \
  "$ROOT/tests/ProtocolSelfTest.java"
java -cp "$TMP_DIR" dev.rcplaygrounds.microview.ProtocolSelfTest
# Validate firmware syntax without needing real MicroView/Arduino libraries.
g++ -std=gnu++11 -Wall -Wextra -Werror -I "$ROOT/tests/mock_arduino" \
    -x c++ -fsyntax-only "$ROOT/firmware/rc_microview/rc_microview.ino"
echo 'OK: firmware syntax (using dummy Arduino headers)'
g++ -std=gnu++11 -Wall -Wextra -Werror -I "$ROOT/tests/mock_arduino" \
    "$ROOT/tests/FirmwareLogicTest.cpp" -o "$TMP_DIR/firmware_logic"
"$TMP_DIR/firmware_logic"