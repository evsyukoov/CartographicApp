FROM openjdk:8-jdk-alpine

RUN apk update
RUN apk add -y maven
COPY pom.xml /usr/local/service/pom.xml
COPY src /usr/local/service/src
WORKDIR /usr/local/service
RUN mvn package
CMD ["java","-jar","target/TransformBot-1.0-SNAPSHOT.jar"]