plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board)
            implementation(projects.hds.model)

            implementation(libs.kotlin.serialization.core)
        }
    }
}
