plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.watchparty.common)

    implementation(projects.hds.model)
    implementation(projects.server.service)

    implementation(projects.utils.types)

    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.json)

    implementation(libs.logging)
}
