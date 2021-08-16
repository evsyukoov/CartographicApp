#!/bin/bash

touch supervisor_log
echo "Monitoring start" >> supervisor_log
while true
   do
   process1=$(ps aux | grep -v grep | grep IngGeoBot-1.0.jar)
   process2=$(ps aux | grep -v grep | grep GeodeticBot-1.0.jar)

    if [ -z "$process1" ];then
      NOW=$(date +"%Y-%m-%d;%H-%M-%S")
      echo "$NOW" >> supervisor_log
      echo "IngGeoBot was failed..restarting..." >> supervisor_log
      cd CartographicApp2 && nohup java -jar target/IngGeoBot-1.0.jar &
      cd ~/
    fi

    if [ -z "$process2" ];then
      NOW=$(date +"%Y-%m-%d;%H-%M-%S")
      echo "$NOW" >> supervisor_log
      echo "GeodeticBot was failed..restarting..." >> supervisor_log
      cd CartographicApp && nohup java -jar target/GeodeticBot-1.0.jar &
      cd ~/
    fi

    sleep 60
 done