FROM gradle:8.10-jdk23-alpine AS build
WORKDIR /app
COPY . .
RUN gradle :worker:build -x test

FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/worker/build/libs/worker*.jar app.jar
CMD ["java", "-jar", "app.jar"]