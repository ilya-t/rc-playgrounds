## Before you start
`src/main.py` defines constants for network and PWM behavior. Update them to
match your setup before running the controller:

- `UDP_IP`, `UDP_PORT` – address and port to listen for control packets.
- `STREAM_DST_IP` – IP of the client that will receive the H.264 UDP stream.
- `PWM_YAW_PIN`, `PWM_PITCH_PIN`, `PWM_STEER_PIN`, `PWM_LONG_PIN` – Raspberry Pi
  GPIO pins (BCM numbering) driving yaw, pitch, steering and throttle servos.
- `PWM_GIMB_MODE`, `PWM_GIMB_SENS` – pins that set gimbal mode and
  sensitivity; their pulse widths are fixed to `GIMB_MODE_VAL` and
  `GIMB_SENS_VAL`.
- `PWM_FREQUENCY`, `PWM_MIN`, `PWM_MAX` – base PWM frequency and pulse width
  range for yaw/pitch/steer channels.
- `PWM_MIN_LONG`, `PWM_MAX_LONG` – pulse width range for the throttle channel.
- `NO_DATA_TIMEOUT` – seconds before servo pulses are cleared when no control
  packets are received.
Adjust these values to match your hardware and network environment.

## Quickstart
```sh
./run.sh
```

## Launch at start
## pigpiod.service for camera
- create `/etc/systemd/system/pigpiod.service`:
```
[Unit]
Description=Pigpio daemon
After=network.target

[Service]
Type=forking
PIDFile=/var/run/pigpio.pid
ExecStart=/usr/bin/pigpiod
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=2

[Install]
WantedBy=multi-user.target
```

- enable it:
```sh
sudo systemctl daemon-reload
sudo systemctl enable pigpiod.service
sudo systemctl start pigpiod.service
```
show journals if something is wrong: `journalctl -u pigpiod.service`

## service for fpv app
- create `/etc/systemd/system/fpv_controller.service`:
```
[Unit]
Description=fpv controller startup script
After=pigpiod.service
Requires=pigpiod.service

[Service]
ExecStart=/home/fpvcar/rc-playgrounds/fpv_controller/run.sh > /tmp/fpv_controller.log
Restart=always
User=fpvcar
Group=fpvcar
WorkingDirectory=/usr/local/bin
StandardOutput=append:/tmp/fpv_controller_service.log
StandardError=append:/tmp/fpv_controller_service.log

[Install]
WantedBy=multi-user.target
```

- enable it:
```sh
sudo systemctl daemon-reload
sudo systemctl enable fpv_controller.service
sudo systemctl start fpv_controller.service
```

# Fake video stream from existing raspberry binary
```sh
cd src
python3 fake_video_stream.py |
gst-launch-1.0 \
    fdsrc ! \
    h264parse ! \
    rtph264pay ! \
    udpsink \
    host=192.168.1.48 \
    port=12345 \
```

Or:
```sh
./run --dry-run
```