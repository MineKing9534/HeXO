plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.discord.oauth2.model)

            implementation(libs.bundles.ktor.client)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            runtimeOnly(libs.ktor.client.cio)
        }
    }

    sourceSets.jsMain {
        dependencies {
            runtimeOnly(libs.ktor.client.js)
        }
    }
}
