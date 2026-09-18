plugins {
    id("kotlin-jvm")
    id("application")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.launcher)
    implementation(projects.database)

    implementation(projects.game.hds)

    implementation(projects.board)
    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(projects.discord.linkedRoles)
    implementation(projects.discord.oauth2.service)

    implementation(projects.server)
    implementation(projects.server.api)

    implementation(projects.watchparty.service)

    runtimeOnly(libs.logback)
    runtimeOnly(libs.r2dbc.pool)
}

application {
    mainClass = "de.mineking.hexo.launcher.api.MainKt"
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
}
