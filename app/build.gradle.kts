plugins {
    kotlin("jvm") version "1.9.22"
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("AppKt")
}

kotlin {
    jvmToolchain(17)
}
