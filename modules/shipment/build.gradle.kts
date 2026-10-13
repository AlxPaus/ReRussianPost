plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":modules:pricing"))
}
