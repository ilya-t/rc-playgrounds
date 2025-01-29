#!/bin/bash
set -e
SAMPLE=/app/sample.mp4
if [ ! -f $SAMPLE ]; then
    echo "File $SAMPLE does not exist!"
    exit 1
fi

trap "echo 'Exiting...'; exit" SIGINT

echo "Will playback: $SAMPLE in loop!"

playback() {
    gst-launch-1.0 -v \
        filesrc location=$SAMPLE ! \
        qtdemux name=demux demux.video_0 ! \
        queue ! \
        h264parse config-interval=1 ! \
        rtph264pay config-interval=1 pt=96 ! \
        udpsink host=127.0.0.1 port=12345
}

if [ -z "$NO_LOOP" ]; then
    while true; do
        playback
        echo "Playback completed, will restart in a second..."
        sleep 1
    done
else
    playback
    echo "Playback completed."
fi
done
