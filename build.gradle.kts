plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "ru.posttrack"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}