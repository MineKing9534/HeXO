import de.mineking.discord.localization.gradle.import

plugins {
    id("kotlin-jvm")

    alias(libs.plugins.dtk.localization)
}

dependencies {
    implementation(projects.board)
    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(projects.hds.model)
    implementation(projects.discord.link)

    implementation(projects.server.service)
    implementation(projects.utils.coroutines)

    implementation(libs.kotlin.coroutines.core)

    implementation(libs.cache)

    implementation(libs.jda)
    implementation(libs.jda.emoji)
    implementation(libs.dtk)

    implementation(libs.logging)

    runtimeOnly(kotlin("reflect"))
}

discordLocalization {
    locales = listOf("en-US")
    defaultLocale = "en-US"

    localizationDirectory = "$projectDir/localization"
    locationFormat = "%locale%/%name%.yaml"

    botPackage = "de.mineking.hexo.bot"

    import("kotlin.math.roundToInt")

    import("de.mineking.hexo.hds.model.TimeControl")
    import("de.mineking.hexo.hds.model.game.FinishedGame")
    import("de.mineking.hexo.hds.model.game.GameFinishReason")
    import("de.mineking.hexo.hds.model.profile.RichProfile")

    import("kotlin.time.toJavaInstant")
    import("net.dv8tion.jda.api.utils.TimeFormat")
}
