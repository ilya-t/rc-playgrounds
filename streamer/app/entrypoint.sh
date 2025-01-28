#!/bin/bash
set -e
SAMPLE=$(ls /samples/*.mp4 | head -n 1)
echo "Will playback: $SAMPLE"

gst-launch-1.0 -v \
filesrc location=$SAMPLE ! \
qtdemux name=demux demux.video_0 ! \
queue ! \
h264parse config-interval=1 ! \
rtph264pay config-interval=1 pt=96 ! \
udpsink host=127.0.0.1 port=12345
