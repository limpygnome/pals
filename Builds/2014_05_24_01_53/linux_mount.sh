#!/bin/bash

mkdir /pals/shared
chmod -R 777 /pals/shared

mount -t cifs //10.0.0.150/shared /pals/shared -o dir_mode=0777,file_mode=0777
