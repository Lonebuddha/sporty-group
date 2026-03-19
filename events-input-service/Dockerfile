FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY events-input-service/pom.xml events-input-service/pom.xml
COPY events-bets-matching-service/pom.xml events-bets-matching-service/pom.xml
COPY events-input-service/src events-input-service/src

RUN mvn -pl events-input-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/events-input-service/target/events-input-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "mkdir -p /logs && exec java ${JAVA_OPTS} -jar /app/app.jar"]
