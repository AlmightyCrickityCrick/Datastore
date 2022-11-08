FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:11
ARG config config
RUN mkdir /app
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-docker-sample.jar
COPY --from=build /home/gradle/src/${config} ./config

ENTRYPOINT ["java","-jar","/app/ktor-docker-sample.jar"]