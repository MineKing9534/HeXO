plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.game.model)
            api(projects.discord.core)

            implementation(libs.kotlin.serialization.core)
        }
    }
}
