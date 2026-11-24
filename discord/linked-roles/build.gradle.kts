import net.dv8tion.jda.api.interactions.DiscordLocale

plugins {
    id("kotlin-jvm")
    alias(libs.plugins.dtk)
}

dependencies {
    implementation(projects.database)
    implementation(projects.discord.core)
    implementation(projects.discord.link)
    implementation(projects.discord.oauth2.service)
    implementation(projects.game.model)
    implementation(projects.utils.coroutines)
    implementation(projects.utils.types)

    implementation(libs.jda)
    implementation(libs.kotlin.coroutines.core)

    implementation(libs.logging)
}

discordToolKit {
    core {}
    localization {
        locales.addAll(DiscordLocale.ENGLISH_US)
        basePackage = "de.mineking.hexo.discord.linkedroles"
    }
}
