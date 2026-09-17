plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.utils.cache)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.core)

            implementation(libs.svg)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(projects.board.parse)
        }
    }

    sourceSets.jvmTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }
}
