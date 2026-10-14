# Build: docker compose build backend  (hoac: docker build -t tripmind-backend backend/)
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
RUN useradd --system --uid 1001 tripmind
COPY --from=build /app/target/tripmind-backend-*.jar app.jar
USER tripmind
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8088
# Image temurin co san curl
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:${SERVER_PORT:-8088}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
