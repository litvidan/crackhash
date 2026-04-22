plugins {
    kotlin("jvm")
}

dependencies {
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JSON serialization (used for RabbitMQ messages)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // RabbitMQ client
    implementation("com.rabbitmq:amqp-client:5.20.0")

    // Testing
    testImplementation(kotlin("test"))
}


kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}