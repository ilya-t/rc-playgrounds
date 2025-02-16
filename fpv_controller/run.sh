#!/bin/bash
set -e
FPS=90
# FPS=60
# FPS=30
# WIDTH=640
# HEIGHT=480

WIDTH=320
HEIGHT=240

# Resolve the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "===> Starting service at $SCRIPT_DIR" >> /tmp/fpv_controller.log

# Navigate to the script directory
cd "$SCRIPT_DIR"

echo "===> Starting control service"
python3 src/main.py >> /tmp/fpv_controller_control.log 2>&1 &

echo "===> Starting stream"
raspivid \
    -pf baseline \
    -awb cloud \
    -fl \
    -g 1 \
    -w $WIDTH \
    -h $HEIGHT \
    --nopreview \
    -fps 90/1  \
    -t 0 \
    -o - | \
gst-launch-1.0 \
    fdsrc ! \
    h264parse ! \
    rtph264pay ! \
    udpsink \
    host=192.168.2.5 \
    port=12345 \
>> /tmp/fpv_controller_control_stream.log &

