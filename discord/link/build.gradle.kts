plugins {
    id("kotlin-jvm")

    alias(libs.plugins.ksp)
}

dependencies {
    implementation(projects.discord.core)
    implementation(projects.game.model)

    implementation(projects.utils.types)

    implementation(projects.database)
    ksp(projects.database.processor)

    implementation(libs.bundles.exposed)
}
