#!/bin/sh

mkdir logs
mkdir src/main/resources/uploaded
mkdir src/main/resources/send

sed  -i 's_Users/denis/Denis/DxfParser_home/denis/DxfConverter/converter_g' src/main/java/convert/DXFConverter.java
mvn package
nohup java -jar target/TransformBot-1.0-SNAPSHOT.jar &