#!/bin/bash
TARGET_CONFIG=$1

if [ "$TARGET_CONFIG" == "" ]; then
    echo "Usage: $0 /path/to/wg.conf"
    exit 1
fi

# Check if the file does NOT exist as a regular file
if [ ! -f "$TARGET_CONFIG" ]; then
    echo "Config '$TARGET_CONFIG' not exists!"
    exit 1
fi

mkrdir -p ./config
set -e
cp $TARGET_CONFIG ./config/wg0.conf
chmod 600 ./config/wg0.conf

docker compose up --detach
