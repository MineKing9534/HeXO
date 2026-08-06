plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.utils.omissible)
            implementation(libs.kotlin.serialization.core)
        }
    }
}
