plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            api(projects.utils.types)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }
}
