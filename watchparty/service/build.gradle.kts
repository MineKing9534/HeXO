plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.atomicfu)
}

dependencies {
    implementation(projects.watchparty.protocol)

    implementation(projects.game.model)
    implementation(projects.server.service)

    implementation(projects.utils.types)

    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.json)

    implementation(libs.logging)
}
