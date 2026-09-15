plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.utils.types)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)

    api(libs.socketio.server)
    runtimeOnly(libs.jakarta.servlet)

    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.coroutines.core)

    implementation(libs.logging)
}
