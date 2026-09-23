package de.mineking.hexo.game.implementation.service.auth

import de.mineking.hexo.database.NanoId
import de.mineking.hexo.database.NanoIdTable
import de.mineking.hexo.discord.core.discordUserIdColumn
import de.mineking.hexo.game.implementation.service.profile.ProfileTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

@JvmInline
@Serializable
internal value class PrincipalId(val value: String)

@Serializable
internal enum class PrincipalType {
    User,
}

internal data object PrincipalTable : NanoIdTable<PrincipalId>("principals") {
    override fun NanoId.wrapId() = PrincipalId(value)
    override fun PrincipalId.unwrapId() = NanoId(value)

    val discordId = discordUserIdColumn("discord_id").nullable().uniqueIndex()
    val profileId = reference("profile_id", ProfileTable.id).uniqueIndex()

    val type = enumerationByName<PrincipalType>("type", 4)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
