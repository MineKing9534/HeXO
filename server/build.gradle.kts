plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.server.api)

    implementation(libs.bundles.ktor.server)
    implementation(libs.logging)
}
