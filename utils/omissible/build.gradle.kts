plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.serialization.core)
        }
    }
}
