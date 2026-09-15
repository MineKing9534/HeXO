@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.discord.linkedroles

import de.mineking.discord.DiscordToolKit
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.read
import de.mineking.hexo.discord.linkedroles.localization.LinkedRolesLocalizationManager
import de.mineking.hexo.discord.oauth2.updateLinkedRoleMetadata

interface LinkedRolesMetadataLocalization : LocalizationFile

suspend fun DiscordToolKit<*>.installLinkedRoleMetadata() {
    val localization = LinkedRolesLocalizationManager(this).read<LinkedRolesMetadataLocalization>()
    updateLinkedRoleMetadata(localization, listOf(RankKey, EloKey))
}
