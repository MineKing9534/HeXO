plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.ktor.client.core)

            implementation(libs.socketio.client)

            implementation(libs.logging)
        }
    }
}
