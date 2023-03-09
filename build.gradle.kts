plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    `java-library`
}

group = "com.volodya7292"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(18)
}

dependencies {
    implementation("net.java.dev.jna:jna:5.13.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
