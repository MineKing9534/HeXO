plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)

    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.discord.bot)
    implementation(projects.discord.link)
    implementation(projects.server)

    implementation(projects.board)
    implementation(projects.board.parse)
    implementation(projects.board.parse.hds)
    implementation(projects.board.render)

    implementation(projects.hds.implementation)

    implementation(projects.sync.service)

    implementation(libs.ktor.server.html)
    implementation(libs.cache)

    implementation(libs.kotlin.serialization.properties)

    implementation(libs.logging)
    runtimeOnly(libs.logback)

    runtimeOnly(libs.postgres)
}

application {
    mainClass = "de.mineking.hexo.launcher.MainKt"
}
