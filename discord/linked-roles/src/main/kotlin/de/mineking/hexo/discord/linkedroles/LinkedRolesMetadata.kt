@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.discord.linkedroles

import de.mineking.discord.await
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.localization.LocalizationMessageGroup
import de.mineking.discord.localization.runtime.TranslationManager
import de.mineking.discord.localization.source.yaml.YamlCatalogLoader
import de.mineking.hexo.discord.oauth2.LinkedRoleMetadataKey
import de.mineking.hexo.discord.oauth2.LinkedRoleMetadataType
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.RoleConnectionMetadata
import net.dv8tion.jda.api.interactions.DiscordLocale

internal abstract class LocalizedLinkRoleMetadataKey<T : Any>(name: String, type: LinkedRoleMetadataType<T>) : LinkedRoleMetadataKey<T>(name, type) {
    abstract fun LinkedRolesMetadataLocalization.localize(): LinkedRoleMetadataLocalization
}

internal object RankKey : LocalizedLinkRoleMetadataKey<Int>("rank", LinkedRoleMetadataType.IntegerLessThanOrEqual) {
    override fun LinkedRolesMetadataLocalization.localize() = rank()
}
internal object EloKey : LocalizedLinkRoleMetadataKey<Int>("elo", LinkedRoleMetadataType.IntegerGreaterThanOrEqual) {
    override fun LinkedRolesMetadataLocalization.localize() = elo()
}
internal object OverallGamesKey : LocalizedLinkRoleMetadataKey<Int>("overall_games", LinkedRoleMetadataType.IntegerGreaterThanOrEqual) {
    override fun LinkedRolesMetadataLocalization.localize() = overallGames()
}

@LocalizationBundle("linkedRoles")
internal interface LinkedRolesMetadataLocalization {
    fun rank(): LinkedRoleMetadataLocalization
    fun elo(): LinkedRoleMetadataLocalization
    fun overallGames(): LinkedRoleMetadataLocalization
}

@LocalizationMessageGroup
internal interface LinkedRoleMetadataLocalization {
    fun name(): String
    fun description(): String
}

suspend fun JDA.installLinkedRoleMetadata() {
    val translationManager = TranslationManager(DiscordLocale.ENGLISH_US, YamlCatalogLoader.resources())
    updateLinkedRoleMetadata(translationManager, listOf(RankKey, EloKey, OverallGamesKey))
}

private suspend fun JDA.updateLinkedRoleMetadata(
    translationManager: TranslationManager,
    data: List<LocalizedLinkRoleMetadataKey<*>>,
) {
    updateRoleConnectionMetadata(data.map { entry ->
        val localization = translationManager.localized<LinkedRolesMetadataLocalization, _> {
            entry.run { localize() }
        }

        RoleConnectionMetadata(entry.type.type, localization.defaultValue.name(), entry.key, localization.defaultValue.description())
            .setNameLocalizations(localization.translations.mapValues { it.value.name() })
            .setDescriptionLocalizations(localization.translations.mapValues { it.value.description() })
    }).await()
}
