# Monorepo build — requires dockerContext at repo root (see render-hospitality.yaml).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend/libs/fixna-platform-common/pom.xml backend/libs/fixna-platform-common/pom.xml
COPY backend/libs/fixna-platform-common/src backend/libs/fixna-platform-common/src
COPY backend/hospitality-api/pom.xml backend/hospitality-api/pom.xml
COPY backend/hospitality-api/src backend/hospitality-api/src
RUN mvn -N -f pom.xml install \
    && mvn -f backend/libs/fixna-platform-common/pom.xml -DskipTests install \
    && mvn -f backend/hospitality-api/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
USER appuser
COPY --from=build /workspace/backend/hospitality-api/target/fixna-hospitality-api.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV FIXNA_APP_ENV=prod
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]