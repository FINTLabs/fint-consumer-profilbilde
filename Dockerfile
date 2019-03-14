FROM gradle:4.6.0-jdk8-alpine as builder
USER root
COPY . .
ARG apiVersion
RUN gradle --no-daemon -PapiVersion=${apiVersion} build

FROM gcr.io/distroless/java
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-consumer-profilbilde-*.jar /data/fint-consumer-profilbilde.jar
CMD ["/data/fint-consumer-profilbilde.jar"]
