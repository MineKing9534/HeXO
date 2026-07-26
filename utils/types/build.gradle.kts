plugins {
    id("latex")
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.serialization.core)
        }
    }
}
