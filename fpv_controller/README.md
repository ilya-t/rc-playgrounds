## Quickstart
```sh
./run.sh
```


## Launch at start
- create `startup.service`:
```
[Unit]
Description=Startup Script
After=network.target

[Service]
ExecStart=/home/pi/rc-playgrounds/fpv_controller/run.sh
Restart=always
User=pi
Group=pi
WorkingDirectory=/usr/local/bin
StandardOutput=append:/var/log/fpv_controller.log
StandardError=append:/var/log/fpv_controller.log

[Install]
WantedBy=multi-user.target
```

- enable it:
```sh
sudo cp fpv_controller/startup.service /etc/systemd/system/startup.service
sudo systemctl daemon-reload
sudo systemctl enable startup.service
```
