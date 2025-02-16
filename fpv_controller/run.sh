#!/bin/bash
set -e
FPS=90
# FPS=60
# FPS=30
# WIDTH=640
# HEIGHT=480

WIDTH=320
HEIGHT=240

FPV_CONTROLLER_LOG=/tmp/fpv_controller.log
FPV_CONTROLLER_CONTROL_LOG=/tmp/fpv_controller_control.log

# Resolve the directory of the script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "===> Starting service at $SCRIPT_DIR" >> $FPV_CONTROLLER_LOG

# Navigate to the script directory
cd "$SCRIPT_DIR"

# echo "===> Starting stream"
# raspivid \
#     -pf baseline \
#     -awb cloud \
#     -fl \
#     -g 1 \
#     -w $WIDTH \
#     -h $HEIGHT \
#     --nopreview \
#     -fps $FPS/1  \
#     -t 0 \
#     -o - | \
# gst-launch-1.0 \
#     fdsrc ! \
#     h264parse ! \
#     rtph264pay ! \
#     udpsink \
#     host=192.168.2.5 \
#     port=12345 \
# >> $FPV_CONTROLLER_CONTROL_STREAM_LOG &

echo "===> Starting control service"
python3 src/main.py >> $FPV_CONTROLLER_CONTROL_LOG &

echo "Logs: "
# echo "    tail $FPV_CONTROLLER_CONTROL_STREAM_LOG"
echo "    tail $FPV_CONTROLLER_CONTROL_LOG"
echo "    tail $FPV_CONTROLLER_LOG"
