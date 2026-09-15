plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board)
            api(projects.watchparty.model)
            implementation(projects.game.model)

            implementation(libs.kotlin.serialization.core)
        }
    }
}
