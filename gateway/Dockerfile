# Сборка из корня монорепозитория remark-platform:
#   docker build -f gateway/Dockerfile .

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY remark-common-events/pom.xml remark-common-events/pom.xml
COPY reference-service/pom.xml reference-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY remark-service/pom.xml remark-service/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml

RUN mvn -q dependency:go-offline -DskipTests || true

COPY . .

RUN mvn -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/gateway/target/gateway-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
