# ─── Stage 1: Build ───────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom first (layer cache — only re-downloads deps if pom changes)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Make mvnw executable and pre-download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build the JAR (skip tests — tests run in CI, not in Docker build)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built JAR from the build stage (lean image)
COPY --from=build /app/target/*.jar app.jar

# Railway injects PORT env var automatically
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
