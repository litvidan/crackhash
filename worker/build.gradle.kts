plugins {
    kotlin("jvm")
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

    // For RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.20.0")

    // For JSON serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    // Common project classes
    implementation(project(":common"))
}

application {
    mainClass.set("com.litvidan.worker.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
