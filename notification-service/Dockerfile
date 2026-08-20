# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the complete Maven reactor metadata.
COPY pom.xml .
COPY common/pom.xml common/
COPY employee-service/pom.xml employee-service/
COPY auth-service/pom.xml auth-service/
COPY notification-service/pom.xml notification-service/

# notification-service depends on shared correlation infrastructure
# from the common module.
COPY common/src common/src
COPY notification-service/src notification-service/src

RUN mvn -pl notification-service -am clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S -g 10001 ems \
    && adduser -S -D -H -u 10001 -G ems ems

WORKDIR /app

COPY --from=build \
    --chown=10001:10001 \
    /build/notification-service/target/notification-service-*.jar \
    app.jar

USER 10001:10001

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]