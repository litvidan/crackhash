FROM gradle:8.10-jdk23-alpine AS build
WORKDIR /app
COPY . .
RUN gradle :gateway:build -x test

FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/gateway/build/libs/gateway*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]