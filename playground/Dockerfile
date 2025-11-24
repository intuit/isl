# Build stage
FROM gradle:8.5-jdk21 AS build

# Install Node.js for frontend build
RUN apt-get update && apt-get install -y nodejs npm && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the entire playground directory
COPY . .

# Build the application (this will build frontend first, then backend)
WORKDIR /app/backend
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/backend/build/libs/isl-playground-1.0.0.jar /app/app.jar

# Expose port (Railway will override this with PORT env var)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/app.jar"]

