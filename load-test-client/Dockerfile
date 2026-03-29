FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY build/libs/load-test-client-1.0.0-all.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
