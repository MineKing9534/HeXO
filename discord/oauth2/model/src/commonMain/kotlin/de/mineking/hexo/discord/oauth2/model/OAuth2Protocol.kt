package de.mineking.hexo.discord.oauth2.model

import kotlinx.serialization.Serializable

@Serializable
data class OAuth2AuthorizationResponse(val url: String)

@Serializable
data class OAuth2CallbackResponse(val success: Boolean, val flow: OAuth2Flow?)
