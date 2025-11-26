# ============================
# 1) Build stage (Maven)
# ============================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Workdir inside the container for the build
WORKDIR /workspace

# Copy pom.xml first (better layer caching)
COPY pom.xml .

# Download dependencies (optional but speeds up rebuilds)
RUN mvn -q -DskipTests dependency:go-offline

# Now copy the source code
COPY src ./src

# Build the Spring Boot jar (skip tests for faster Docker build)
RUN mvn -q -DskipTests package


# ============================
# 2) Runtime stage (JRE only)
# ============================
FROM eclipse-temurin:17-jre-jammy

# Directory where the app will run
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/target/clustered-data-warehouse-0.0.1-SNAPSHOT.jar app.jar

# Expose the HTTP port (inside container)
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
