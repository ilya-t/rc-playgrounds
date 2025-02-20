#!/bin/bash
set -e

# Resolve the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "===> Starting service at $SCRIPT_DIR"
cd "$SCRIPT_DIR"
echo "===> Starting control service"
python3 src/main.py $1
