# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

# Cache Maven dependencies separately from source changes
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

RUN addgroup -S medical && adduser -S medical -G medical && \
    mkdir -p /app/uploads /app/backups && \
    apk add --no-cache mysql-client wget && \
    chown -R medical:medical /app

COPY --from=build /app/target/medical-billing-system-*.jar app.jar

USER medical

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/login || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
