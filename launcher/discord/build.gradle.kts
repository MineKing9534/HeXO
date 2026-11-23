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

    implementation(projects.discord.bot)
    implementation(projects.discord.config)
    implementation(projects.discord.link)
    implementation(projects.discord.linkedRoles)
    implementation(projects.discord.oauth2.service)

    implementation(libs.jda)
    implementation(libs.dtk.localization)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.core)

    implementation(libs.logging)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.r2dbc.pool)
}

application {
    mainClass = "de.mineking.hexo.launcher.discord.MainKt"
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("localization/catalog.index") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
    append("localization/catalog.index")
}
