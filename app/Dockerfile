FROM docker.io/eclipse-temurin:24-jdk-alpine
LABEL maintainer="TheMeinerLP"
LABEL description="Docker container for the Titan Lobby"
LABEL type="Microtus"

RUN mkdir /app
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

ARG MAPS
ADD ${MAPS} /app/worlds

ENTRYPOINT ["java", "-jar", "app.jar"]