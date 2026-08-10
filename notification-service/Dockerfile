# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the complete Maven reactor metadata.
COPY pom.xml .
COPY common/pom.xml common/
COPY employee-service/pom.xml employee-service/
COPY auth-service/pom.xml auth-service/
COPY notification-service/pom.xml notification-service/

# This service has no source dependency on common.
COPY notification-service/src notification-service/src

RUN mvn -pl notification-service -am clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build \
    /build/notification-service/target/notification-service-*.jar \
    app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]