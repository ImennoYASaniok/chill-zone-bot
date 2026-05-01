plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    implementation(project(":data"))
    api("com.github.kotlin-telegram-bot:kotlin-telegram-bot:6.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(17) }


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name.contains("Test", ignoreCase = true)) {
        enabled = false
    }
}

tasks.withType<Test>().configureEach {
    enabled = false
}