# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Resolve dependencies before copying source so normal code changes reuse this layer.
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S solaris && adduser -S -G solaris solaris
WORKDIR /app
COPY --from=build --chown=solaris:solaris /workspace/build/libs/*.jar app.jar

USER solaris
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
