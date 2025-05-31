plugins {
    kotlin("jvm") version "2.0.10"
    application
}

application.mainClass = "CLIKt"

group = "io.github.zerumi"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")

    implementation(project(":lang-backend"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}