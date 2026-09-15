package de.mineking.hexo.launcher

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> loadConfig(): T = Properties.decodeFromStringMap(System.getenv())

@Serializable
data class DiscordOAuth2Config(
    val clientId: String,
    val clientSecret: String,
    val encryptionKey: String,
)

@Serializable
data class DatabaseConfig(val url: String)
