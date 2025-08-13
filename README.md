# RC Playgrounds

This repository contains a small FPV remote control experiment.  It is split into two
independent pieces: a controller running on a Raspberry Pi and an Android
application used as the client.

## Setup overview

### 1. FPV controller at Raspberry Pi
The script starts the video stream and control loop. 
Please see [fpv_controller/README.md](fpv_controller/README.md)
for details and setup under your environment.


### 2. Android control app
The is client app that listens for video stream and sends control signals to rc.
See: [control_app/README.md](control_app/README.md).
APKs are available at: https://github.com/ilya-t/rc-playgrounds/releases
