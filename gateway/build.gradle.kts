plugins {
    kotlin("jvm")
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

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // MongoDB Kotlin Coroutine Driver
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.0")

    // RabbitMQ Client
    implementation("com.rabbitmq:amqp-client:5.20.0")

    // For JSON serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")

    testImplementation(kotlin("test"))

    // Common project classes
    implementation(project(":common"))
}

application {
    mainClass.set("com.litvidan.gateway.MainKt")
}
