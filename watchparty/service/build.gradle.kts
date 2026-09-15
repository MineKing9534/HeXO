plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.atomicfu)
}

dependencies {
    implementation(projects.utils.socketio.server)
    implementation(projects.watchparty.protocol)

    implementation(projects.game.model)
    implementation(projects.server.api)

    implementation(projects.utils.types)

    implementation(libs.logging)
}
