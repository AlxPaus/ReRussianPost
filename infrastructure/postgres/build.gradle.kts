plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikari)
    runtimeOnly(libs.postgresql)
}
