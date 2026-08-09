# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package \
    && cp target/chatspringai-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S sillage && adduser -S sillage -G sillage

WORKDIR /app
COPY --from=build --chown=sillage:sillage /workspace/app.jar app.jar

USER sillage
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=45s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
