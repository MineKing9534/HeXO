package de.mineking.hexo.launcher.discord

import de.mineking.hexo.launcher.DatabaseConfig
import de.mineking.hexo.launcher.DiscordOAuth2Config
import kotlinx.serialization.Serializable

@Serializable
data class BotApplicationConfig(
    val bot: DiscordBotConfig,
    val oauth2: DiscordOAuth2Config? = null,
    val server: PublicServerConfig? = null,
    val database: DatabaseConfig? = null,
)

@Serializable
data class DiscordBotConfig(val token: String)

@Serializable
data class PublicServerConfig(val url: String)
