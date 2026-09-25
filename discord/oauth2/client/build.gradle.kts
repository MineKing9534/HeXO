plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.jsMain {
        dependencies {
            api(projects.discord.oauth2.model)

            implementation(libs.bundles.ktor.client)
            runtimeOnly(libs.ktor.client.js)
        }
    }
}
