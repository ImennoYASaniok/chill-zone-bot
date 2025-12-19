plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")

    testImplementation(kotlin("test"))
    implementation(project(":data"))
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

kotlin {
    jvmToolchain(17)
}

