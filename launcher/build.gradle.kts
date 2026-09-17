plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.database)
    implementation(project(projects.database.path, configuration = "r2dbc"))

    implementation(projects.discord.oauth2.service)
    implementation(projects.game.hds)
    implementation(projects.game.model)

    implementation(libs.kotlin.serialization.properties)

    runtimeOnly(libs.r2dbc.pool)
    runtimeOnly(libs.logback)
}
