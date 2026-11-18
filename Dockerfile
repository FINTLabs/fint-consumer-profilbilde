FROM eclipse-temurin:8-jdk AS builder
WORKDIR /workspace
COPY . .
ARG apiVersion
ARG buildFlags=""
RUN ./gradlew --no-daemon ${buildFlags} -PapiVersion=${apiVersion} build

FROM eclipse-temurin:8-jre
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"
COPY --from=builder /workspace/build/deps/external/*.jar /data/
COPY --from=builder /workspace/build/deps/fint/*.jar /data/
COPY --from=builder /workspace/build/libs/fint-consumer-profilbilde-*.jar /data/fint-consumer-profilbilde.jar
ENTRYPOINT ["java", "-jar", "/data/fint-consumer-profilbilde.jar"]
