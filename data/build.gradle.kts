plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Ktor dependencies
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // SLF4J logging implementation
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}