plugins {
    id("kotlin-multiplatform")

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.game.implementation.protocol)

            implementation(projects.utils.socketio.client)
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
