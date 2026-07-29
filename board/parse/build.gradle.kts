plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)

            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }
}
