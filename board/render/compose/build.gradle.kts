plugins {
    kotlin("multiplatform")
    id("kotlin-common")

    alias(libs.plugins.kotlin.compose.compiler)

    id("publish")
}

repositories {
    google()
}

kotlin {
    js { browser() }

    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.board.render)

            implementation(libs.bundles.compose.html)

            implementation(libs.kotlin.serialization.core)
        }
    }
}
