@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)
}

kotlin {
    withSourcesJar(publish = true)

    sourceSets.commonMain {
        generatedKotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        dependencies {
            api(projects.game.model)
            implementation(projects.board)

            implementation(projects.utils.coroutines)
            implementation(projects.utils.socketio.client)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.client.websockets)

            implementation(libs.logging)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            runtimeOnly(libs.ktor.client.cio)
        }
    }

    sourceSets.jsMain {
        dependencies {
            runtimeOnly(libs.ktor.client.js)
        }
    }
}
