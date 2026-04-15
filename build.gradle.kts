buildscript {
    extra["kotlin_dream_group"] = "io.github.dreammooncai"
    extra["kotlin_dream_version"] = "1.0.3"
}
plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

allprojects {
    group = rootProject.extra["kotlin_dream_group"].toString()
    version = rootProject.extra["kotlin_dream_version"].toString()
}