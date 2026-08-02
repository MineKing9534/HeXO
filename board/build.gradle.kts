plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")
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
