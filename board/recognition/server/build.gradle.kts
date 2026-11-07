plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.board.recognition)
    implementation(projects.server)
    implementation(libs.kotlin.serialization.json)

    testImplementation(projects.board.recognition.client)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.ktor.server)
}
