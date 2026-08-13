package de.mineking.hexo.discord.core

import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.UserSnowflake

@JvmInline
@Serializable
value class DiscordUserId(val value: Long) : UserSnowflake {
    private val delegate get() = UserSnowflake.fromId(value)

    override fun getDefaultAvatarId() = delegate.defaultAvatarId
    override fun getAsMention() = delegate.asMention
    override fun getIdLong() = value
}
