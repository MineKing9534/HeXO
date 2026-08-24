@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)

    alias(libs.plugins.ksp)
}

dependencies {
    kspCommonMainMetadata(projects.game.hds.processor)
}

kotlin {
    withSourcesJar(publish = true)

    sourceSets.commonMain {
        generatedKotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        dependencies {
            api(projects.game.model)
            implementation(projects.board)

            implementation(projects.utils.coroutines)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.bundles.ktor.client)

            implementation(libs.logging)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
            implementation(libs.ktor.client.cio)

            implementation("io.socket:socket.io-client:2.1.1")
        }
    }

    sourceSets.jsMain {
        dependencies {
            implementation(libs.ktor.client.js)

            implementation(npm("socket.io-client", "4.8.3"))
        }
    }
}

project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

project.tasks.matching { it.name.lowercase().endsWith("sourcesjar") }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}
