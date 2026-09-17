plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.coroutines.core)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }
}
