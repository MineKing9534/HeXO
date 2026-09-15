package de.mineking.hexo.launcher.api

import de.mineking.hexo.launcher.DatabaseConfig
import de.mineking.hexo.launcher.DiscordOAuth2Config
import kotlinx.serialization.Serializable

@Serializable
data class ApiApplicationConfig(
    val oauth2: DiscordOAuth2Config? = null,
    val server: ServerConfig,
    val database: DatabaseConfig? = null,
)

@Serializable
data class ServerConfig(val port: Int, val url: String)
