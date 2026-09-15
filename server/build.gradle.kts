plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.server.api)

    implementation(libs.bundles.ktor.server)

    implementation(projects.utils.socketio.server)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.cors)

    implementation(libs.logging)
}
