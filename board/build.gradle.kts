plugins {
    id("kotlin-multiplatform")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.core)

            api(libs.kotlin.serialization.core)
        }
    }
}
