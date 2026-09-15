package de.mineking.hexo.discord.oauth2.protocol

import kotlinx.serialization.Serializable

@Serializable
data class OAuth2AuthorizationResponse(val url: String)

@Serializable
data class OAuth2CallbackResponse(val success: Boolean)
