plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.game.model)

            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }
}
