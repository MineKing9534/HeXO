plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.coroutines.core)

            implementation(libs.logging)
        }
    }
}
