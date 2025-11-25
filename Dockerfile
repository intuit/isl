# Build stage
FROM eclipse-temurin:21-jdk AS build

# Install Node.js for frontend build
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the playground directory
COPY playground playground

# Copy gradle wrapper and build files from root
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle.properties .

# Copy ISL transform library (needed for backend build)
COPY isl-transform isl-transform

# Build frontend
WORKDIR /app/playground/frontend
RUN if [ -f "package.json" ]; then \
      echo "Building frontend..."; \
      npm install && npm run build && \
      mkdir -p ../backend/src/main/resources/static && \
      cp -r dist/* ../backend/src/main/resources/static/ && \
      echo "Frontend built successfully"; \
    else \
      echo "No frontend package.json found, skipping frontend build"; \
    fi

# Build backend
WORKDIR /app/playground/backend
RUN chmod +x ../../gradlew && ../../gradlew clean build -x test --no-daemon

# Runtime stage - Use Java 21 JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/playground/backend/build/libs/isl-playground-1.0.0.jar /app/app.jar

# Expose port
EXPOSE 8080

# Run with Java 21
CMD ["java", "-jar", "/app/app.jar"]

