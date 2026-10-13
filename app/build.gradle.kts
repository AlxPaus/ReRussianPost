plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("ru.posttrack.app.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":modules:identity"))
    implementation(project(":modules:pricing"))
    implementation(project(":modules:shipment"))
    implementation(project(":modules:tracking"))
    implementation(project(":modules:warehouse"))
    implementation(project(":infrastructure:postgres"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback.classic)
}
