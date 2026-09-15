plugins {
    id("kotlin-jvm")
    id("application")

    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.launcher)
    implementation(projects.database)

    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(projects.discord.bot)
    implementation(projects.discord.config)
    implementation(projects.discord.link)
    implementation(projects.discord.linkedRoles)
    implementation(projects.discord.oauth2.service)

    implementation(projects.game.hds)

    implementation(libs.dtk)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.core)

    runtimeOnly(libs.logback)
    runtimeOnly(libs.r2dbc.pool)
}

application {
    mainClass = "de.mineking.hexo.launcher.discord.MainKt"
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
}
