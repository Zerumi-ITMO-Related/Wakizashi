plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "2.1.20"
}

group = "io.github.zerumi"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}