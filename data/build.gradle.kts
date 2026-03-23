plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}