# Monorepo build — requires dockerContext at repo root (see render-localboost.yaml).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend/libs/fixna-platform-common/pom.xml backend/libs/fixna-platform-common/pom.xml
COPY backend/libs/fixna-platform-common/src backend/libs/fixna-platform-common/src
COPY backend/localboost-api/pom.xml backend/localboost-api/pom.xml
COPY backend/localboost-api/src backend/localboost-api/src
RUN mvn -f pom.xml -pl backend/localboost-api -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
USER appuser
COPY --from=build /workspace/backend/localboost-api/target/fixna-localboost-api.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV FIXNA_APP_ENV=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
