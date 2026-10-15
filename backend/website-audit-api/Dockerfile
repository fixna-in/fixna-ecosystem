# Monorepo build — requires dockerContext at repo root (see render-website-audit.yaml).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend/libs/fixna-platform-common/pom.xml backend/libs/fixna-platform-common/pom.xml
COPY backend/libs/fixna-platform-common/src backend/libs/fixna-platform-common/src
COPY backend/website-audit-api/pom.xml backend/website-audit-api/pom.xml
COPY backend/website-audit-api/src backend/website-audit-api/src
RUN mvn -N -f pom.xml install \
    && mvn -f backend/libs/fixna-platform-common/pom.xml -DskipTests install \
    && mvn -f backend/website-audit-api/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
USER appuser
COPY --from=build /workspace/backend/website-audit-api/target/fixna-website-audit-api.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV FIXNA_APP_ENV=prod
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/api/v1/health/ready || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
