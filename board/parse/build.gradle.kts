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

            implementation(projects.utils.cache)

            implementation(libs.kotlin.serialization.core)
        }
    }
}
