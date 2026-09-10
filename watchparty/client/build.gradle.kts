plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.utils.socketio.client)
            api(projects.watchparty.model)
            implementation(projects.watchparty.protocol)
            implementation(projects.game.model)

            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.client.websockets)

            implementation(libs.logging)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}
