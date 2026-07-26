plugins {
    id("latex")
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.utils.types)
            implementation(libs.kotlin.serialization.core)
        }
    }
}
