# Build: docker compose build backend  (hoac: docker build -t tripmind-backend backend/)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 tripmind
COPY --from=build /app/target/tripmind-backend-*.jar app.jar
USER tripmind
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
