FROM gradle:8.5-jdk23 AS build
WORKDIR /app
COPY . .
RUN gradle :gateway:build -x test

FROM openjdk:23-slim
WORKDIR /app
COPY --from=build /app/gateway/build/libs/gateway-*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]