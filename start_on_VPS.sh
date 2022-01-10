#!/bin/sh
#для старта на сервере

mkdir logs
mkdir src/main/resources/uploaded
mkdir src/main/resources/send

sed  -i 's_Users/denis/Denis/DxfParser/converter_home/denis/DxfConverter/converter_g' src/main/java/convert/DXFConverter.java
#убиваем старую инсталяцию
port=$(ps aux | grep java |  sed 's/   */ /g' | cut -d " "  -f2 | head -n 1)
kill -9 "$port"
#заменяем параметры подключения к БД (скрипт лежит на сервере)
sh ../start_scrypt.sh
#сборка и старт
mvn package
nohup java -jar target/TransformBot-1.2.0-SNAPSHOT.jar &