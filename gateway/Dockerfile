# Stage 1: Build the application using a multi-stage build for optimized, clean builds.

# Use a base image with the correct Gradle version.
FROM gradle:9.0-jdk17-alpine AS build
WORKDIR /home/gradle/project

# --- Install gcompat ---
# The protoc-gen-grpc-java plugin is compiled against glibc, but Alpine uses musl.
# gcompat provides a compatibility layer to run glibc-based applications on Alpine.
RUN apk add --no-cache gcompat

# --- Step 1: Copy build configuration ---
COPY build.gradle.kts settings.gradle.kts ./
COPY gateway/build.gradle.kts ./gateway/
COPY worker/build.gradle.kts ./worker/

# --- Step 2: Download dependencies ---
RUN gradle :gateway:dependencies :worker:dependencies --no-daemon

# --- Step 3: Copy source code ---
COPY gateway/src ./gateway/src
COPY worker/src ./worker/src

# --- Step 4: Build the application ---
RUN gradle :gateway:installDist --no-daemon

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the installed application from the build stage
COPY --from=build /home/gradle/project/gateway/build/install/gateway .

# The command to run the application
CMD ["./bin/gateway"]
