plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)

            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlin.coroutines.core)
        }
    }
}
