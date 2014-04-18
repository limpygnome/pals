#!/bin/bash

cp pals.service /etc/systemd/system/
systemctl daemon-reload
echo Installed service. Type "systemctl start pals" to start the daemon.
echo Note: type "systemctl enable pals" to enable automatic startup.
