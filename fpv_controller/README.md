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
- create `startup.service`:
```
[Unit]
Description=Startup Script
After=network.target wg-quick@wg0.service
Wants=wg-quick@wg0.service

[Service]
ExecStart=/home/pi/rc-playgrounds/fpv_controller/run.sh
Restart=always
User=pi
Group=pi
WorkingDirectory=/usr/local/bin
StandardOutput=append:/tmp/fpv_controller.log
StandardError=append:/tmp/fpv_controller.log

[Install]
WantedBy=multi-user.target
```

- enable it:
```sh
sudo cp ./startup.service /etc/systemd/system/startup.service
sudo systemctl daemon-reload
sudo systemctl enable startup.service
sudo systemctl start startup.service
```

## Fake video stream from existing raspberry binary
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