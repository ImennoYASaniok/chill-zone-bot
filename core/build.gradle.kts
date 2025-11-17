plugins {
    kotlin("jvm") version "1.9.22"
}

dependencies {
    // тут зависимости core модуля
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
