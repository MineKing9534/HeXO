plugins {
    id("kotlin-jvm")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.hds.model)

    implementation(projects.database)
    ksp(projects.database.processor)

    implementation(libs.jda)
    implementation(libs.dtk)

    implementation(libs.bundles.exposed)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
}
