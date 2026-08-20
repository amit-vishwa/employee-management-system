# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the complete Maven reactor metadata.
COPY pom.xml .
COPY common/pom.xml common/
COPY employee-service/pom.xml employee-service/
COPY auth-service/pom.xml auth-service/
COPY notification-service/pom.xml notification-service/

# Copy only the source required for this image.
COPY common/src common/src
COPY auth-service/src auth-service/src

RUN mvn -pl auth-service -am clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S -g 10001 ems \
    && adduser -S -D -H -u 10001 -G ems ems

WORKDIR /app

COPY --from=build \
    --chown=10001:10001 \
    /build/auth-service/target/auth-service-*.jar \
    app.jar

USER 10001:10001

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]