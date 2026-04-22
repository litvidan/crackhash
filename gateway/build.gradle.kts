plugins {
    kotlin("jvm")
    application
}

dependencies {
    // Shared module
    implementation(project(":common"))

    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:2.3.10")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.10")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-gson:2.3.10")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-ktor-server:7.0.0")

    // MongoDB Kotlin Coroutine Driver
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // Testing
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.litvidan.gateway.MainKt")
}
