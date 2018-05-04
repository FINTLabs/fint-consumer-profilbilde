FROM gradle:4.6.0-jdk8-alpine as builder
USER root
COPY . .
ARG apiVersion
RUN gradle --no-daemon -PapiVersion=${apiVersion} build

FROM openjdk:8-jre-alpine
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-consumer-avatar-*.jar /data/fint-consumer-avatar.jar
CMD ["java", "-jar", "/data/fint-consumer-avatar.jar"]
