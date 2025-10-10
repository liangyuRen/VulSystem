# Multi-stage build for VulSystem Backend
# Stage 1: Build the application
FROM maven:3.8.5-openjdk-8 AS builder

WORKDIR /build

# Copy Maven configuration
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw.cmd .

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:8-jre-alpine

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/uploads /app/opensca /app/logs

# Copy the built jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /app
USER appuser

# Expose the application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
