plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.coroutines.core)

            implementation(libs.logging)
        }
    }
}
