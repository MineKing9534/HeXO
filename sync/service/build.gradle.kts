plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.sync.common)

    implementation(projects.hds)
    implementation(projects.server.service)

    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.json)

    implementation(libs.logging)
}
