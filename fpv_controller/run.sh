#!/bin/bash
set -e
FPV_CONTROL_LOG=/tmp/fpv_controller_control.log

# Resolve the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "===> Starting service at $SCRIPT_DIR"

# Navigate to the script directory
cd "$SCRIPT_DIR"
echo "===> Starting control service"
python3 src/main.py
