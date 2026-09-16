import de.mineking.discord.localization.gradle.import

plugins {
    id("kotlin-jvm")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dtk.localization)
}

dependencies {
    implementation(projects.discord.core)
    implementation(projects.discord.config)
    implementation(projects.discord.link)
    implementation(projects.discord.oauth2.service)

    implementation(projects.board)
    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(projects.game.model)

    implementation(projects.server.api)
    implementation(projects.utils.coroutines)
    implementation(projects.utils.types)
    implementation(projects.utils.cache)

    implementation(libs.kotlin.coroutines.core)

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
    managerName = "$botPackage.localization.HexoBotLocalizationManager"

    import("kotlin.math.roundToInt")

    import("de.mineking.hexo.game.model.TimeControl")
    import("de.mineking.hexo.game.model.game.FinishedGame")
    import("de.mineking.hexo.game.model.game.GameFinishReason")
    import("de.mineking.hexo.game.model.profile.ProfileWithStatistics")
    import("de.mineking.hexo.game.model.profile.ProfileGameStatistics")
    import("de.mineking.hexo.game.model.profile.Profile")

    import("kotlin.time.toJavaInstant")
    import("net.dv8tion.jda.api.utils.TimeFormat")
}
