plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)

    id("tailwindcss")
}

dependencies {
    api(projects.server.service)

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.html)

    implementation(libs.logging)
}

tailwindcss {
    resourceTask = tasks.processResources
    resourcePath = "static"
}
