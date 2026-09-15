package de.mineking.hexo.discord.oauth2

import de.mineking.discord.DiscordToolKit
import de.mineking.discord.commands.LocalizationInfo
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.utils.await
import net.dv8tion.jda.api.entities.RoleConnectionMetadata
import net.dv8tion.jda.api.entities.RoleConnectionMetadata.MetadataType
import kotlin.time.Instant

sealed class LinkedRoleMetadataType<T : Any>(val type: MetadataType) {
    object IntegerLessThanOrEqual : LinkedRoleMetadataType<Int>(MetadataType.INTEGER_LESS_THAN_OR_EQUAL)
    object IntegerGreaterThanOrEqual : LinkedRoleMetadataType<Int>(MetadataType.INTEGER_GREATER_THAN_OR_EQUAL)
    object IntegerEqual : LinkedRoleMetadataType<Int>(MetadataType.INTEGER_EQUALS)
    object IntegerNotEqual : LinkedRoleMetadataType<Int>(MetadataType.INTEGER_NOT_EQUALS)
    object DateTimeLessThanOrEqual : LinkedRoleMetadataType<Instant>(MetadataType.DATETIME_LESS_THAN_OR_EQUAL)
    object DateTimeGreaterThanOrEqual : LinkedRoleMetadataType<Instant>(MetadataType.DATETIME_GREATER_THAN_OR_EQUAL)
    object BooleanEqual : LinkedRoleMetadataType<Boolean>(MetadataType.BOOLEAN_EQUAL)
    object BooleanNotEqual : LinkedRoleMetadataType<Boolean>(MetadataType.BOOLEAN_NOT_EQUAL)
}

abstract class LinkedRoleMetadataKey<T : Any>(val key: String, val type: LinkedRoleMetadataType<T>)

data class LinkedRoleMetadataValue<T : Any>(val key: LinkedRoleMetadataKey<T>, val value: T?)
fun <T : Any> LinkedRoleMetadataKey<T>.bindValue(value: T?) = LinkedRoleMetadataValue(this, value)

suspend fun DiscordToolKit<*>.updateLinkedRoleMetadata(
    localization: LocalizationFile,
    data: List<LinkedRoleMetadataKey<*>>,
) {
    jda.updateRoleConnectionMetadata(data.map {
        val namePackage = localization.createLinkedRoleMetadataLocalization(it.key, "name")
        val descriptionPackage = localization.createLinkedRoleMetadataLocalization(it.key, "description")

        RoleConnectionMetadata(it.type.type, namePackage.default, it.key, descriptionPackage.default)
            .setNameLocalizations(namePackage.localization)
            .setDescriptionLocalizations(descriptionPackage.localization)
    }).await()
}

private fun LocalizationFile.createLinkedRoleMetadataLocalization(key: String, suffix: String): LocalizationInfo {
    val localization = manager.locales.associateWith { readString("linked_roles.$key.$suffix", it) }
    return LocalizationInfo(localization[manager.defaultLocale]!!, localization)
}
