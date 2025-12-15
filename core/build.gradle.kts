plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
