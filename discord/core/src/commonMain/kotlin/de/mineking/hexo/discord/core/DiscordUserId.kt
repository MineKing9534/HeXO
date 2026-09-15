package de.mineking.hexo.discord.core

import kotlinx.serialization.Serializable

@Serializable
expect value class DiscordUserId(val value: Long)
