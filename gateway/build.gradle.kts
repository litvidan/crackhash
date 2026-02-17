plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    application
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-gson:2.3.10")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-ktor-server:7.0.0")

    // gRPC - Using BOM and the standard grpc-netty artifact to get DnsNameResolverProvider
    implementation(platform("io.grpc:grpc-bom:1.62.2"))
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("io.grpc:grpc-protobuf")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.1")
    implementation("io.grpc:grpc-netty") // Use non-shaded for DNS resolver
    implementation("io.grpc:grpc-services")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

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

application {
    mainClass.set("com.litvidan.gateway.MainKt")
}
