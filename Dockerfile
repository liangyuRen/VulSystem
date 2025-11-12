# Multi-stage build for VulSystem Backend
# Stage 1: Build the application
FROM maven:3.8.5-openjdk-8 AS builder

WORKDIR /build

# Copy Maven settings for Aliyun mirror (for faster builds in China)
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

# Copy Maven configuration
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
# Using Eclipse Temurin for better security and long-term support
FROM eclipse-temurin:8-jre-alpine

# Set working directory
WORKDIR /app

# Install curl for health checks and create necessary directories
RUN apk add --no-cache curl && \
    mkdir -p /app/uploads /app/opensca /app/logs

# Copy the built jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Set default JVM options (can be overridden by environment variables)
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose the application port
EXPOSE 8081

# Health check using curl instead of wget
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application with JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
