package de.mineking.hexo.launcher.web

import kotlinx.serialization.Serializable

@Serializable
data class WebApplicationConfig(
    val server: ServerConfig,
)

@Serializable
data class ServerConfig(val port: Int)
