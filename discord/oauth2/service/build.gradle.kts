plugins {
    id("kotlin-jvm")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.discord.oauth2.model)

    implementation(projects.server.api)

    implementation(projects.database)
    ksp(projects.database.processor)

    implementation(projects.utils.coroutines)
    implementation(projects.utils.types)

    implementation(libs.bundles.exposed)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    runtimeOnly(libs.ktor.client.cio)

    implementation(libs.cache)
    implementation(libs.jda)
    implementation(libs.dtk)

    testImplementation(kotlin("test"))
}
