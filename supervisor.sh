#!/bin/sh

while true
do
  process1=$(ps aux | grep -v grep | grep target/IngGeoBot-1.0.jar)
  process2=$(ps aux | grep -v grep | grep target/GeodeticBot-1.0.jar)

  if [ -z "$process1" ];then
    NOW=$(date +"%Y-%m-%d;%H-%M-%S")
    echo "$NOW"
    echo "IngGeoBot was failed..restarting..."
    nohup java -jar target/IngGeoBot-1.0.jar &
  fi

  if [ -z "$process2" ];then
    NOW=$(date +"%Y-%m-%d;%H-%M-%S")
    echo "$NOW"
    echo "GeodeticBot was failed..restarting..."
    nohup java -jar target/GeodeticBot-1.0.jar &
  fi

  sleep 60
done