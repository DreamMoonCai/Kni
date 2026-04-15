import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary as NativeLib

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    id("maven-publish")
    alias(libs.plugins.maven.publish)
}

val dreamLib: KotlinNativeTarget.() -> Unit = {
    compilations.getByName("main") {
        cinterops {
            val jni by creating {
                defFile(project.file("src/cppMain/jni.def"))
            }
        }
    }
    binaries {
        fun NativeLib.lib() {
            baseName = "KDreamKniLib"
        }
        if (konanTarget.family.isAppleFamily) framework {
            isStatic = true
            lib()
        }
        staticLib { lib() }
        sharedLib { lib() }
    }
}

kotlin {
    jvm("desktop")
    androidLibrary {
        namespace = "io.github.dreammooncai.kni"
        compileSdk = 36
        minSdk = 26
        compilerOptions.jvmTarget.set(JvmTarget.valueOf("JVM_${JavaVersion.current().majorVersion}"))
    }
    macosX64(dreamLib)
    macosArm64(dreamLib)
    iosX64(dreamLib)
    iosArm64(dreamLib)
    iosSimulatorArm64(dreamLib)
    mingwX64("windows", dreamLib)
    androidNativeArm64(dreamLib)
    androidNativeArm32(dreamLib)
    androidNativeX86(dreamLib)
    androidNativeX64(dreamLib)

    applyDefaultHierarchyTemplate()

    sourceSets {

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlin.logging)
            implementation(libs.kotlinx.serialization)
            implementation(libs.dream.everything)
            implementation(kotlin("reflect"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val jvmMain by creating {
            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(jvmMain)
        }
        androidMain.dependencies {
            api(libs.root.su.core)
            api(libs.root.su.service)
            api(libs.root.su.nio)
            implementation(libs.androidx.annotation.jvm)
            implementation(libs.yukireflection.api.kotlin)
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
        }

        desktopMain.dependencies {
            implementation(libs.slf4j.api)
        }

        val desktopNativeMain by creating {
            dependsOn(nativeMain.get())
        }

        macosMain {
            dependsOn(desktopNativeMain)
        }

        val windowsMain by getting {
            dependsOn(desktopNativeMain)
        }

        all {
            languageSettings.enableLanguageFeature("ContextParameters")
            compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            compilerOptions.freeCompilerArgs.add("-Xallow-contracts-on-more-functions")
        }
    }
}


mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(),"kni-api" , rootProject.extra["kotlin_dream_version"].toString())

    pom {
        name.set("Kni")
        description.set("A quick solution for intercommunication with Kotlin/JVM in Kotlin/Native, encapsulating JNI to accommodate more modern Kotlin call methods.")
        url.set("https://github.com/DreamMoonCai/Kni/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("DreamMoonCai")
                name.set("dreammoon")
                url.set("https://github.com/DreamMoonCai/")
            }
        }
        scm {
            url.set("https://github.com/DreamMoonCai/Kni")
            connection.set("scm:git:git://github.com/DreamMoonCai/Kni.git")
            developerConnection.set("scm:git:ssh://git@github.com/DreamMoonCai/Kni.git")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri("../local-plugin-repository")
        }
    }
}