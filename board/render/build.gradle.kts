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

            implementation(libs.kotlin.serialization.core)
            implementation(libs.svg)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
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
