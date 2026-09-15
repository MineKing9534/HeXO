plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")

    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.serialization.core)
            implementation(libs.kotlin.coroutines.core)
        }
    }
}
