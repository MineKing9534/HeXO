package de.mineking.hexo.link.oauth2

import de.mineking.discord.DiscordToolKit
import de.mineking.discord.commands.LocalizationInfo
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.read
import de.mineking.discord.utils.await
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.RoleConnectionMetadata
import net.dv8tion.jda.api.entities.RoleConnectionMetadata.MetadataType
import kotlin.reflect.KClass
import kotlin.time.Instant

sealed class LinkedRoleMetadataType<T : Any>(
    internal val clazz: KClass<T>,
    internal val type: MetadataType,
) {
    object IntegerLessThanOrEqual : LinkedRoleMetadataType<Int>(Int::class, MetadataType.INTEGER_LESS_THAN_OR_EQUAL)
    object IntegerGreaterThanOrEqual : LinkedRoleMetadataType<Int>(Int::class, MetadataType.INTEGER_GREATER_THAN_OR_EQUAL)
    object IntegerEqual : LinkedRoleMetadataType<Int>(Int::class, MetadataType.INTEGER_EQUALS)
    object IntegerNotEqual : LinkedRoleMetadataType<Int>(Int::class, MetadataType.INTEGER_NOT_EQUALS)
    object DateTimeLessThanOrEqual : LinkedRoleMetadataType<Instant>(Instant::class, MetadataType.DATETIME_LESS_THAN_OR_EQUAL)
    object DateTimeGreaterThanOrEqual : LinkedRoleMetadataType<Instant>(Instant::class, MetadataType.DATETIME_GREATER_THAN_OR_EQUAL)
    object BooleanEqual : LinkedRoleMetadataType<Boolean>(Boolean::class, MetadataType.BOOLEAN_EQUAL)
    object BooleanNotEqual : LinkedRoleMetadataType<Boolean>(Boolean::class, MetadataType.BOOLEAN_NOT_EQUAL)
}

data class LinkedRoleMetadataKey<T : Any>(val key: String, val type: LinkedRoleMetadataType<T>)

data class LinkedRoleMetadataValue<T : Any>(val key: LinkedRoleMetadataKey<T>, val value: T?)
fun <T : Any> LinkedRoleMetadataKey<T>.bindValue(value: T?) = LinkedRoleMetadataValue(this, value)

suspend inline fun <reified L : LocalizationFile> DiscordToolKit<*>.updateLinkedRoleMetadata(vararg data: LinkedRoleMetadataKey<*>) {
    val localization = localizationManager.read<L>()
    jda.updateLinkedRoleMetadata(localization, data = data)
}

@PublishedApi
internal suspend fun JDA.updateLinkedRoleMetadata(
    localization: LocalizationFile,
    vararg data: LinkedRoleMetadataKey<*>,
) {
    updateRoleConnectionMetadata(data.map { (key, type) ->
        val namePackage = localization.createLinkedRoleMetadataLocalization(key, "name")
        val descriptionPackage = localization.createLinkedRoleMetadataLocalization(key, "description")

        RoleConnectionMetadata(type.type, namePackage.default, key, descriptionPackage.default)
            .setNameLocalizations(namePackage.localization)
            .setDescriptionLocalizations(descriptionPackage.localization)
    }).await()
}

private fun LocalizationFile.createLinkedRoleMetadataLocalization(key: String, suffix: String): LocalizationInfo {
    val localization = manager.locales.associateWith { readString("linked_roles.$key.$suffix", it) }
    return LocalizationInfo(localization[manager.defaultLocale]!!, localization)
}
