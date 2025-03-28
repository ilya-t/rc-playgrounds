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