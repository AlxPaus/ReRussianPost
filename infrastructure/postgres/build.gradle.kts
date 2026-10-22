plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":modules:identity"))
    implementation(project(":modules:pricing"))
    implementation(project(":modules:shipment"))
    implementation(project(":modules:tracking"))
    implementation(project(":modules:warehouse"))

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.hikari)
    runtimeOnly(libs.postgresql)
}
