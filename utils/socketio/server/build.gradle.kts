plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)

    implementation(libs.socketio.server)
}
