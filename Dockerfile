FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

ENV LOG_PATH=/app/logs

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appointmentgroup \
    && adduser -S appointmentuser -G appointmentgroup

COPY --from=build /app/target/*.jar app.jar

RUN chown appointmentuser:appointmentgroup app.jar

USER appointmentuser

EXPOSE 8080

ENV LOG_PATH=/app/logs

ENTRYPOINT ["java", "-jar", "app.jar"]