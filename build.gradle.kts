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

subprojects {
    val projectBuildPath = path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir(projectBuildPath))

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
