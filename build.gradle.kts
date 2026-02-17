plugins {
    kotlin("jvm") version "2.3.0"
    id("com.google.protobuf") version "0.9.4" apply false
}

group = "org.litvidan"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}
