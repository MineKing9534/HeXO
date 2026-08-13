package de.mineking.hexo.link.database

import de.mineking.hexo.discord.core.DiscordUserIdTable
import de.mineking.hexo.link.oauth2.Scope
import org.jetbrains.exposed.v1.core.EnumerationNameColumnType
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DiscordUserTokensTable : DiscordUserIdTable("discord_user_tokens", "discord_id") {
    val accessToken = blob("access_token")
    val refreshToken = blob("refresh_token")
    val expiresAt = timestamp("expires_at")
    val scopes = array("scopes", EnumerationNameColumnType(Scope::class, 20))
}
