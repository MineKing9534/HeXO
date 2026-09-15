@file:Suppress("MatchingDeclarationName", "Filename")

package de.mineking.hexo.discord.oauth2.model

import de.mineking.hexo.discord.core.DiscordUserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("discord-token-create")
data class DiscordTokenStoreDatabaseEvent(val user: DiscordUserId, val cause: OAuth2Flow)
