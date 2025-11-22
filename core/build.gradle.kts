plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    // тут зависимости core модуля
    testImplementation(kotlin("test"))
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
}

kotlin {
    jvmToolchain(17)
}
