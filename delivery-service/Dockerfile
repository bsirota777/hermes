# Build context must be the repo root (not this directory), since this is a
# Gradle multi-module build and Gradle needs settings.gradle + every
# subproject's build.gradle to resolve project(':common'):
#   docker build -f delivery-service/Dockerfile -t hermes-delivery-service .

# --- Build stage ---
FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

# Gradle configures every project listed in settings.gradle regardless of
# which task you run, so every subproject's build.gradle needs to exist here
# even though only delivery-service's source gets copied below.
COPY common/build.gradle common/build.gradle
COPY user-service/build.gradle user-service/build.gradle
COPY delivery-service/build.gradle delivery-service/build.gradle
COPY profile-service/build.gradle profile-service/build.gradle
COPY wallet-service/build.gradle wallet-service/build.gradle

RUN ./gradlew :delivery-service:dependencies --no-daemon || true

COPY common ./common
COPY delivery-service ./delivery-service

RUN ./gradlew :delivery-service:bootJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk upgrade --no-cache p11-kit p11-kit-trust
WORKDIR /app
COPY --from=build /app/delivery-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
