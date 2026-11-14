plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            api(projects.utils.types)

            implementation(projects.discord.core)
            implementation(projects.utils.cache)

            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlin.serialization.core)
        }
    }
}
