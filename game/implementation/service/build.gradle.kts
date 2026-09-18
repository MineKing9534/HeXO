plugins {
    id("kotlin-jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(projects.game.implementation.protocol)

    implementation(projects.database)
    ksp(projects.database.processor)

    implementation(projects.server.api)

    implementation(libs.bundles.exposed)
}
