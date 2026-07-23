# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy parent POM and module POMs first (better layer caching)
COPY pom.xml .
COPY common/pom.xml common/
COPY employee-service/pom.xml employee-service/

# Copy source
COPY common/src common/src
COPY employee-service/src employee-service/src

# Build only what's needed (common + employee-service)
RUN mvn -pl common,employee-service -am clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/employee-service/target/employee-service-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]