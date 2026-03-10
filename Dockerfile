# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY guestbot-core/pom.xml guestbot-core/
COPY guestbot-repository/pom.xml guestbot-repository/
COPY guestbot-service/pom.xml guestbot-service/
COPY guestbot-telegram/pom.xml guestbot-telegram/
COPY guestbot-api/pom.xml guestbot-api/
COPY guestbot-app/pom.xml guestbot-app/

# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B 2>/dev/null || true

COPY . .
RUN mvn clean package -DskipTests -B

# Runtime image — минимальный
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user
RUN addgroup -S guestbot && adduser -S guestbot -G guestbot
USER guestbot

COPY --from=builder /build/guestbot-app/target/guestbot-app-*.jar app.jar

# JVM tuning для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
