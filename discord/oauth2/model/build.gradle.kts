plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.discord.core)
            implementation(libs.kotlin.serialization.core)
        }
    }
}
