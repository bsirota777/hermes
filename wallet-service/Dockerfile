# Build context must be the repo root (not this directory), since this is a
# Gradle multi-module build and Gradle needs settings.gradle + every
# subproject's build.gradle to resolve project(':common'):
#   docker build -f wallet-service/Dockerfile -t hermes-wallet-service .

# --- Build stage ---
FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

# Gradle configures every project listed in settings.gradle regardless of
# which task you run, so every subproject's build.gradle needs to exist here
# even though only wallet-service's source gets copied below.
COPY common/build.gradle common/build.gradle
COPY user-service/build.gradle user-service/build.gradle
COPY delivery-service/build.gradle delivery-service/build.gradle
COPY profile-service/build.gradle profile-service/build.gradle
COPY wallet-service/build.gradle wallet-service/build.gradle

RUN ./gradlew :wallet-service:dependencies --no-daemon || true

COPY common ./common
COPY wallet-service ./wallet-service

RUN ./gradlew :wallet-service:bootJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk upgrade --no-cache p11-kit p11-kit-trust expat
WORKDIR /app
COPY --from=build /app/wallet-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
