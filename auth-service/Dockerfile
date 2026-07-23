FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/

COPY common/src common/src
COPY auth-service/src auth-service/src

RUN mvn -pl common,auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/auth-service/target/auth-service-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]