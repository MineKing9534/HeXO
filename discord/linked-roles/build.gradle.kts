plugins {
    id("kotlin-jvm")

    alias(libs.plugins.dtk.localization)
}

dependencies {
    implementation(projects.database)
    implementation(projects.discord.core)
    implementation(projects.discord.link)
    implementation(projects.discord.oauth2.service)
    implementation(projects.discord.oauth2.protocol)
    implementation(projects.game.model)
    implementation(projects.utils.coroutines)
    implementation(projects.utils.types)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.dtk)

    implementation(libs.logging)
}

discordLocalization {
    locales = listOf("en-US")
    defaultLocale = "en-US"

    localizationDirectory = "$projectDir/localization"
    locationFormat = "%locale%/%name%.yaml"

    botPackage = "de.mineking.hexo.discord.linkedroles"
    managerName = "$botPackage.localization.LinkedRolesLocalizationManager"
}
