plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")

    id("publish")
    id("kotlin-antlr")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.game.model)

            implementation(projects.utils.cache)

            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }
}
