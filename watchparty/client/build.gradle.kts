plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.watchparty.model)
            implementation(projects.utils.socketio.client)
            implementation(projects.watchparty.protocol)
            implementation(projects.game.model)

            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.client.websockets)

            implementation(libs.logging)
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
