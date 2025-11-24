# Build stage
FROM eclipse-temurin:21-jdk AS build

# Install Node.js for frontend build
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy everything
COPY . .

# Build frontend (conditional - will work even if frontend doesn't exist)
RUN if [ -d "frontend" ]; then \
      echo "Building frontend..."; \
      cd frontend && npm install && npm run build && \
      mkdir -p ../backend/src/main/resources/static && \
      cp -r dist/* ../backend/src/main/resources/static/ && \
      echo "Frontend built successfully"; \
    else \
      echo "No frontend directory found, skipping frontend build"; \
    fi

# Build backend
WORKDIR /app/backend
RUN chmod +x ./gradlew && ./gradlew clean build -x test --no-daemon

# Runtime stage - Use Java 21 JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/backend/build/libs/isl-playground-1.0.0.jar /app/app.jar

# Expose port
EXPOSE 8080

# Run with Java 21
CMD ["java", "-jar", "/app/app.jar"]

