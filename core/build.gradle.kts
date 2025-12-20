plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")

    testImplementation(kotlin("test"))
    implementation(project(":data"))
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

kotlin {
    jvmToolchain(17)
}

