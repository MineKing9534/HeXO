package de.mineking.hexo.discord.oauth2

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
