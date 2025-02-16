#!/bin/bash
set -e

# Resolve the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "===> Starting service at $SCRIPT_DIR" >> /tmp/fpv_controller.log

# Navigate to the script directory
cd "$SCRIPT_DIR"

# Run the Python script
python3 src/main.py >> /tmp/fpv_controller.log 2>&1

