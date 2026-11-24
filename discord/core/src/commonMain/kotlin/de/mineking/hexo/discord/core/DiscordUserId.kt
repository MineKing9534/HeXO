package de.mineking.hexo.discord.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class DiscordUserId(val value: Long)
