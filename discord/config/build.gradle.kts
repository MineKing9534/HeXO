plugins {
    id("kotlin-jvm")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(projects.discord.core)

    implementation(projects.board)
    implementation(projects.board.render)

    implementation(projects.database)
    ksp(projects.database.processor)

    implementation(projects.utils.types)

    implementation(libs.jda)
    implementation(libs.dtk)

    implementation(libs.bundles.exposed)
}
