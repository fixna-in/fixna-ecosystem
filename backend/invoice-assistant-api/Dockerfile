# Monorepo build — requires dockerContext at repo root (see render-invoice-assistant.yaml).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend/libs/fixna-platform-common/pom.xml backend/libs/fixna-platform-common/pom.xml
COPY backend/libs/fixna-platform-common/src backend/libs/fixna-platform-common/src
COPY backend/invoice-assistant-api/pom.xml backend/invoice-assistant-api/pom.xml
COPY backend/invoice-assistant-api/src backend/invoice-assistant-api/src
RUN mvn -N -f pom.xml install \
    && mvn -f backend/libs/fixna-platform-common/pom.xml -DskipTests install \
    && mvn -f backend/invoice-assistant-api/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
USER appuser
COPY --from=build /workspace/backend/invoice-assistant-api/target/fixna-invoice-assistant-api.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV FIXNA_APP_ENV=prod
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/api/v1/health/ready || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
