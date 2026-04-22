FROM gradle:8.5-jdk23 AS build
WORKDIR /app
COPY . .
RUN gradle :worker:build -x test

FROM openjdk:23-slim
WORKDIR /app
COPY --from=build /app/worker/build/libs/worker-*.jar app.jar
CMD ["java", "-jar", "app.jar"]