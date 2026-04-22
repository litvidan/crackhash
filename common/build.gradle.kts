plugins {
    kotlin("jvm")
    id("com.google.protobuf")
}

dependencies {
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kotlinx Serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // RabbitMQ Client
    implementation("com.rabbitmq:amqp-client:5.20.0")

    testImplementation(kotlin("test"))
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

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}