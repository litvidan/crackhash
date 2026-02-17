plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    application
}

dependencies {
    // gRPC - Using BOM (Bill of Materials) to manage all gRPC dependency versions
    implementation(platform("io.grpc:grpc-bom:1.62.2"))
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("io.grpc:grpc-protobuf")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.1")
    implementation("io.grpc:grpc-netty-shaded")

    // For coroutines in gRPC
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

sourceSets {
    main {
        kotlin {
            srcDirs("build/generated/source/proto/main/grpckt", "build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        register("grpc") {
            // This version should align with the BOM
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
        register("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                register("grpc")
                register("grpckt")
            }
        }
    }
}

application {
    mainClass.set("com.litvidan.worker.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
